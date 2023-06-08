package pipelines

pipeline{

    agent { node { label 'evol5-slave' }  }

    parameters{
        string(name: "NetApp_repo", defaultValue: "dummy-network-application", description: "The name of the repository of the network applicaiton to be used." )
        string(name: "NetApp_repo_branch", defaultValue: "main", description: "The name of the branch repository of the NetApp to be used." )
        string(name: 'ROBOT_DOCKER_IMAGE_NAME', defaultValue: 'dockerhub.hi.inet/dummy-netapp-testing/robot-test-image', description: 'Robot Docker image name')
        string(name: 'ROBOT_DOCKER_IMAGE_VERSION', defaultValue: '3.1.1', description: 'Robot Docker image version')
        string(name: 'CAPIF_REGISTRATION_CONFIG_PATH', defaultValue: 'src/python_application/capif_registration.json', description: 'Configuration file for capif registration')
        string(name: 'CERTIFICATES_FOLDER_PATH', defaultValue: 'src/python_application/capif_onboarding', description: 'Folder to store certs after capif registration')
        string(name: 'VERIFICATION_FILE', defaultValue: 'src/python_application/0_network_app_to_nef.py', description: 'Python file that contains functions to verify')
    }

    environment {
        ROOT_DIRECTORY = "${WORKSPACE}/"
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
                    steps {
                        dir ("./capif-services/services") {
                            sh """
                                sed -i "s/image: mongo:6.0.2/image: mongo:4.4.17/g" docker-compose.yml
                                ./run.sh
                            """
                        }
                    }
                }

                stage("Set up nef services."){
                    steps {
                        dir ("./nef-services") {
                            sh """
                                make prepare-dev-env
                                make build-no-cache
                                make upd
                                sleep 30s
                                make db-init
                            """
                        }
                    }
                }

                stage("Set up netapp."){
                    steps {
                        script{
                            if ("$NetApp_repo" == "dummy-network-application") {
                                dir ("$NetApp_repo/src") {
                                    sh """
                                        sed -i 's+"capif_callback_url": "http://192.168.1.13:5000"+"capif_callback_url": "http://host.docker.internal:8086"+g' python_application/capif_registration.json
                                        bash run.sh
                                    """
                                }
                            }else{
                                dir ("$NetApp_repo") {
                                    if (!fileExists('.env')){
                                        sh """
                                            template="\$(find . -name *env*)"
                                            if [ ! -z "\$template" ]; then cp "\$template" .env; else touch .env; fi
                                        """
                                    }
                                    sh """
                                        sed -i 's+"capif_callback_url": "http://192.168.1.13:5000"+"capif_callback_url": "http://host.docker.internal:8086"+g' ${CAPIF_REGISTRATION_CONFIG_PATH}
                                        docker-compose up -d
                                    """
                                }
                            }
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
                                        --add-host host.docker.internal:host-gateway \
                                        -v ${ROOT_DIRECTORY}${NetApp_repo}/:/opt/robot-tests/network-application/ \
                                        -v ${WORKSPACE}/tests:/opt/robot-tests/tests/ \
                                        -v ${WORKSPACE}/libraries:/opt/robot-tests/libraries/ \
                                        -v ${WORKSPACE}/resources:/opt/robot-tests/resources/ \
                                        -v ${WORKSPACE}/results:/opt/robot-tests/results/ \
                                        -v ${WORKSPACE}/tools/credentials.properties:/opt/robot-tests/credentials.properties \
                                        -e FILE_TO_IMPORT=${VERIFICATION_FILE} \
                                        -e CERTIFICATES_FOLDER_PATH=${CERTIFICATES_FOLDER_PATH} \
                                         ${ROBOT_DOCKER_IMAGE_NAME}:${ROBOT_DOCKER_IMAGE_VERSION}  
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
            dir("$NetApp_repo"){
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS'){
                    echo 'Shutdown all network application services'
                    sh """
                        if [ "$NetApp_repo" = "dummy-network-application" ]; then
                            cd src
                            bash cleanup_docker_containers.sh
                            docker network rm demo-network
                        else
                            docker-compose down --rmi all --remove-orphans
                        fi
                    """
                }
                dir ("${env.ROOT_DIRECTORY}/nef-services") {
                    catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS'){
                        echo 'Shutdown all nef services'
                        sh 'docker compose --profile debug down -v'
                    }
                }
                dir ("${env.ROOT_DIRECTORY}/capif-services/services") {
                    catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS'){
                        echo 'Shutdown all capif services'
                        sh './clean_capif_docker_services.sh'
                    }
                }
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
