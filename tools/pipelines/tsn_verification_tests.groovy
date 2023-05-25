package pipelines

pipeline{

    agent { node { label 'evol5-slave' }  }

    parameters{
        string(name: "NetApp_repo", defaultValue: "dummy-network-application", description: "The name of the repository of the Network Application to be used." )
        string(name: "NetApp_repo_branch", defaultValue: "main", description: "The name of the branch repository of the NetApp to be used." )
        string(name: 'ROBOT_DOCKER_IMAGE_NAME', defaultValue: 'dockerhub.hi.inet/dummy-netapp-testing/robot-test-image', description: 'Robot Docker image name')
        string(name: 'ROBOT_DOCKER_IMAGE_VERSION', defaultValue: '3.1.1', description: 'Robot Docker image version')
        string(name: 'SETUP_SCRIPT', defaultValue: 'src/run.sh', description: 'Script to deploy network application')
        string(name: 'UNINSTALL_SCRIPT', defaultValue: 'src/cleanup_docker_containers.sh', description: 'Script to uninstall network application')
        string(name: 'CAPIF_REGISTRATION_CONFIG_PATH', defaultValue: 'src/python_application/capif_registration.json', description: 'Configuration file for capif registration')
        string(name: 'CERTIFICATES_FOLDER_PATH', defaultValue: 'src/python_application/capif_onboarding', description: 'Folder to store certs after capif registration')
        string(name: 'VERIFICATION_FILE', defaultValue: 'src/python_application/0_network_app_to_tsn.py', description: 'Python file that contains functions to verify')
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

        stage("Checkout tsn services"){
            steps{
                checkout([$class: 'GitSCM',
                          branches: [[name: 'main']],
                          doGenerateSubmoduleConfigurations: false,
                          extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "tsn-frontend"]],
                          gitTool: 'Default',
                          submoduleCfg: [],
                          userRemoteConfigs: [[url: 'https://github.com/EVOLVED-5G/TSN_FrontEnd.git']]
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

                stage("Set up tsn frontend."){
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
                        dir ("$NetApp_repo") {
                            sh """
                                sed -i 's+"capif_callback_url": "http://192.168.1.13:5000"+"capif_callback_url": "http://host.docker.internal:8086"+g' ${CAPIF_REGISTRATION_CONFIG_PATH}
                                parentdir=\$(dirname "${SETUP_SCRIPT}")
                                file_to_run=\$(basename -- ${SETUP_SCRIPT})
                                echo \$parentdir
                                echo \$file_to_run
                                cd \$parentdir
                                bash \$file_to_run
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
                                    parentdir=\$(dirname "${SETUP_SCRIPT}")
                                    echo \$parentdir
                                    cp $ROOT_DIRECTORY$NetApp_repo/\$parentdir/.env ${WORKSPACE}/.env
                                    sed -i 's+PATH_TO_CERTS=/usr/src/app/capif_onboarding+PATH_TO_CERTS=/opt/robot-tests/capif_onboarding+g' ${WORKSPACE}/.env
                                    docker run -d -t --name netapp_robot \
                                        --rm --add-host capifcore:host-gateway \
                                        --add-host host.docker.internal:host-gateway \
                                        --env-file ${WORKSPACE}/.env \
                                        -v ${ROOT_DIRECTORY}${NetApp_repo}/${VERIFICATION_FILE}:/opt/robot-tests/netapp_to_tsn.py \
                                        -v ${ROOT_DIRECTORY}${NetApp_repo}/${CERTIFICATES_FOLDER_PATH}:/opt/robot-tests/capif_onboarding \
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
                dir ("${env.ROOT_DIRECTORY}/tsn-frontend") {
                    echo 'Shutdown all tsn services'
                    sh """
                        docker kill tsn-frontend
                        docker rm tsn-frontend
                        docker rmi tsn_frontend:latest
                    """
                }
                dir ("${env.ROOT_DIRECTORY}/capif-services/services") {
                    echo 'Shutdown all capif services'
                    sh './clean_capif_docker_services.sh'
                }

                dir ("$NetApp_repo") {
                    sh """
                        parentdir=\$(dirname "${UNINSTALL_SCRIPT}")
                        file_to_run=\$(basename -- ${UNINSTALL_SCRIPT})
                        echo \$parentdir
                        echo \$file_to_run
                        cd \$parentdir
                        bash \$file_to_run
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