package pipelines

pipeline{

    agent { node { label 'evol5-slave' }  }

    parameters{
        string(name: "NetApp_repo", defaultValue: "dummy-netapp", description: "The name of the repository of the NetApp to be used." )
        string(name: "NetApp_repo_branch", defaultValue: "main", description: "The name of the branch repository of the NetApp to be used." )
        string(name: 'ROBOT_DOCKER_IMAGE_NAME', defaultValue: 'dockerhub.hi.inet/dummy-netapp-testing/robot-test-image', description: 'Robot Docker image name')
        string(name: 'ROBOT_DOCKER_IMAGE_VERSION', defaultValue: '1.0', description: 'Robot Docker image version')
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

        stage("Checkout nef services"){
            when {
                expression { RUN_NEF_LOCALLY == 'true' }
            }
            steps{
                checkout([$class: 'GitSCM',
                          branches: [[name: 'main']],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "nef-services"]],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'https://github.com/EVOLVED-5G/NEF_emulator.git']]
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

                stage("Set up nef services."){
                    when {
                        expression { RUN_NEF_LOCALLY == 'true' }
                    }
                    steps {
                        dir ("./nef-services") {
                            sh """
                                sed -i "s/EXTERNAL_NET=false/EXTERNAL_NET=true/g" env-file-for-local.dev
                                make prepare-dev-env
                                make build
                                make upd
                            """
                        }
                    }
                }

                stage("Set up dummy netapp."){
                    steps {
                        dir ("$NetApp_repo") {
                            sh """
                                sed -i 's+"capif_callback_url": "http://192.168.1.13:5000"+"capif_callback_url": "http://host.docker.internal:8086"+g' pythonnetapp/capif_registration.json
                                ./run.sh
                            """
                        }
                    }
                }

            }

        }
        stage("Verify setup"){
            stages {
                stage("Verify Nef registration to Capif") {
                    steps {
                        dir("./nef-services") {
                            sh """
                                set -e
                                docker-compose ps
                                cp $WORKSPACE/tools/checks/nef_check.sh .
                                chmod +x nef_check.sh
                                ./nef_check.sh
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
                                    docker run -d -t --name netapp_robot \
                                        --rm --add-host capifcore:host-gateway \
                                        -v ${ROOT_DIRECTORY}/${NetApp_repo}/capif_callback_server:/opt/robot-tests/capif-callback \
                                        -v ${ROOT_DIRECTORY}/${NetApp_repo}/nef_callback_server:/opt/robot-tests/nef-callback \
                                        -v ${ROOT_DIRECTORY}/${NetApp_repo}/pythonnetapp:/opt/robot-tests/pythonnetapp \
                                        -v ${WORKSPACE}/tests:/opt/robot-tests/tests/ \
                                        -v ${WORKSPACE}/libraries:/opt/robot-tests/libraries/ \
                                        -v ${WORKSPACE}/resources:/opt/robot-tests/resources/ \
                                        -v ${WORKSPACE}/results:/opt/robot-tests/results/ \
                                        -v ${WORKSPACE}/tools/credentials.properties:/opt/robot-tests/credentials.properties ${ROBOT_DOCKER_IMAGE_NAME}:${ROBOT_DOCKER_IMAGE_VERSION}  
                                """
                            }
                        }
                    }
                }
                stage("Initialize NEF DB"){
                    steps{
                        sh """
                            docker exec -t netapp_robot bash \
                            -c "python3 /opt/robot-tests/libraries/scenario/db-init.py capifcore"
                        """
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
                if(env.RUN_NEF_LOCALLY == 'true'){
                    dir ("${env.ROOT_DIRECTORY}/nef-services") {
                        echo 'Shutdown all nef services'
                        sh 'docker-compose --profile debug down -v --rmi all'
                    }
                }
                if(env.RUN_CAPIF_LOCALLY == 'true'){
                    dir ("${env.ROOT_DIRECTORY}/capif-services/services") {
                        echo 'Shutdown all capif services'
                        sh './clean_capif_docker_services.sh'
                    }
                }

                dir ("$NetApp_repo") {
                    sh """
                        docker-compose down -v --rmi all
                    """
                }
                sh """
                    docker kill netapp_robot
                    docker network rm demo-network
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
