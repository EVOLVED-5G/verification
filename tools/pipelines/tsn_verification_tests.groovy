package pipelines

pipeline{

    agent { node { label 'evol5-slave' }  }

    parameters{
        string(name: "NetApp_repo", defaultValue: "dummy-network-application", description: "The name of the repository of the Network Application to be used." )
        string(name: "NetApp_repo_branch", defaultValue: "main", description: "The name of the branch repository of the NetApp to be used." )
        string(name: 'ROBOT_DOCKER_IMAGE_NAME', defaultValue: 'dockerhub.hi.inet/dummy-netapp-testing/robot-test-image', description: 'Robot Docker image name')
        string(name: 'ROBOT_DOCKER_IMAGE_VERSION', defaultValue: '3.1.1', description: 'Robot Docker image version')
    }

    environment {
        ROOT_DIRECTORY = "${WORKSPACE}/"
        ROBOT_TESTS_DIRECTORY = "${WORKSPACE}/tests"
        ROBOT_RESULTS_DIRECTORY = "${WORKSPACE}/results"
        NEF_SERVICES_ENDPOINT = ""
        CAPIF_SERVICES_ENDPOINT = "http://openshift.evolved-5g.eu/"
        RUN_NEF_LOCALLY = "true"
        RUN_CAPIF_LOCALLY = "true"
    }

    stages{

        stage("Checkout code"){
            steps{
                checkout([$class: 'GitSCM',
                          branches: [[name: '${NetApp_repo_branch}']],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "$NetApp_repo"]],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: "https://github.com/EVOLVED-5G/${NetApp_repo}.git"]]
                ])
            }
        }

        stage("Checkout tsn services"){
            when {
                expression { RUN_CAPIF_LOCALLY == 'true' }
            }
            steps{
                checkout([$class: 'GitSCM',
                          branches: [[name: 'logging']],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "tsn-frontend"]],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'https://github.com/EVOLVED-5G/TSN_FrontEnd.git']]
                ])
            }
        }

        stage("Checkout capif services"){
            when {
                expression { RUN_CAPIF_LOCALLY == 'true' }
            }
            steps{
                checkout([$class: 'GitSCM',
                          branches: [[name: 'develop']],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "capif-services"]],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'https://github.com/EVOLVED-5G/CAPIF_API_Services.git']]
                ])
            }
        }

        stage("Set up environment."){
            stages{

                stage("Set up capif services."){
                    when {
                        expression { RUN_CAPIF_LOCALLY == 'true' }
                    }
                    steps {
                        dir ("./capif-services") {
                            sh """
                                ls -la && cd services
                                sed -i "s/image: mongo:6.0.2/image: mongo:4.4.17/g" docker-compose.yml
                                ./run.sh
                            """
                        }
                    }
                }

                stage("Set up tsn frontend."){
                    when {
                        expression { RUN_NEF_LOCALLY == 'true' }
                    }
                    steps {
                        dir ("./tsn-frontend") {
                            sh """
                                ./build.sh
                                docker run -d --add-host capifcore:host-gateway -p 8899:8899 --name tsn-frontend --restart unless-stopped tsn_frontend
                                docker logs tsn-frontend
                            """
                        }
                    }
                }

                stage("Set up dummy netapp."){
                    steps {
                        dir ("$NetApp_repo/src") {
                            sh """
                                sed -i 's+"capif_callback_url": "http://192.168.1.13:5000"+"capif_callback_url": "http://host.docker.internal:8086"+g' python_application/capif_registration.json
                                ./run.sh
                            """
                        }
                    }
                }

            }

        }
        stage ("Setup Robot FW && Run tests"){
            stages{
                stage("Set up Robot FW container."){
                    steps {
                        dir ("${ROOT_DIRECTORY}") {
                            withCredentials([usernamePassword(
                                    credentialsId: 'docker_pull_cred',
                                    usernameVariable: 'USER',
                                    passwordVariable: 'PASS'
                            )]) {
                                sh """
                                    cp $ROOT_DIRECTORY$NetApp_repo/src/.env ${WORKSPACE}/.env
                                    sed -i 's+PATH_TO_CERTS=/usr/src/app/capif_onboarding+PATH_TO_CERTS=/opt/robot-tests/pythonnetapp/capif_onboarding+g' ${WORKSPACE}/.env
                                    docker run -d -t --name netapp_robot \\
                                        --rm --add-host capifcore:host-gateway \\
                                        --add-host host.docker.internal:host-gateway \\
                                        --env-file ${WORKSPACE}/.env \\
                                        -v ${ROOT_DIRECTORY}${NetApp_repo}/capif_callback_server:/opt/robot-tests/capif-callback \\
                                        -v ${ROOT_DIRECTORY}${NetApp_repo}/nef_callback_server:/opt/robot-tests/nef-callback \\
                                        -v ${ROOT_DIRECTORY}${NetApp_repo}/src/python_application:/opt/robot-tests/pythonnetapp \\
                                        -v ${WORKSPACE}/tests:/opt/robot-tests/tests/ \
                                        -v ${WORKSPACE}/results:/opt/robot-tests/results/ ${ROBOT_DOCKER_IMAGE_NAME}:${ROBOT_DOCKER_IMAGE_VERSION}  
                                """
                            }
                        }
                    }
                }
                stage("Run test cases."){
                    steps{
                        sh """
                            docker exec -t netapp_robot bash -c "robot tests"
                        """
                    }
                }

            }
        }
    }

    post{
        always{
            script {
                if(env.RUN_CAPIF_LOCALLY == 'true'){
                    dir ("${env.ROOT_DIRECTORY}/tsn-frontend") {
                        echo 'Shutdown all tsn services'
                        sh """
                            docker kill tsn-frontend
                            docker rm tsn-frontend
                            docker rmi tsn_frontend:latest
                        """
                    }
                }
                if(env.RUN_CAPIF_LOCALLY == 'true'){
                    dir ("${env.ROOT_DIRECTORY}/capif-services/services") {
                        echo 'Shutdown all capif services'
                        sh './clean_capif_docker_services.sh'
                    }
                }

                dir ("$NetApp_repo/src") {
                    sh """
                        docker compose down -v --rmi all --remove-orphans
                        docker network rm demo-network
                    """
                }
                sh """
                    docker kill netapp_robot
                    docker rmi ${ROBOT_DOCKER_IMAGE_NAME}:${ROBOT_DOCKER_IMAGE_VERSION}
                """
            }

            script {
                echo "Deleting directories."
                cleanWs deleteDirs: true
            }
            echo "Done."
        }
        success{
            echo "Test ran successfully."
        }
    }

}