
String runCapifLocal(String nginxHost) {
    return nginxHost.matches('^(http|https)://localhost.*') ? 'true' : 'false'
}

pipeline{

    agent { node { label 'evol5-slave' }  }

    parameters{
        // nginx-evolved5g.apps-dev.hi.inet
        // string(name: 'NGINX_HOSTNAME', defaultValue: 'http://localhost:8888', description: 'nginx hostname')
        // string(name: "Project_name", defaultValue: "drupal", description: "The name of the project, if upgrade is selected." )
        string(name: "NetApp_repo", defaultValue: "dummy-netapp", description: "The name of the repository of the NetApp to be used." )
        string(name: 'ROBOT_DOCKER_IMAGE_NAME', defaultValue: 'dockerhub.hi.inet/dummy-netapp-testing/robot-test-image', description: 'Robot Docker image name')
        string(name: 'ROBOT_DOCKER_IMAGE_VERSION', defaultValue: '1.0', description: 'Robot Docker image version')
        string(name: 'NEF_HOSTNAME', defaultValue: 'host.docker.internal', description: 'nef services hostname')
        string(name: 'CAPIF_HOSTNAME', defaultValue: 'http://openshift.evolved-5g.eu/', description: 'capif services hostname')
    }

    environment {
        ROOT_DIRECTORY = "${WORKSPACE}/"
        ROBOT_TESTS_DIRECTORY = "${WORKSPACE}/tests"
        ROBOT_RESULTS_DIRECTORY = "${WORKSPACE}/results"
        NEF_SERVICES_ENDPOINT = ""
        CAPIF_SERVICES_ENDPOINT = "http://openshift.evolved-5g.eu/"
        // ROBOT_IMAGE_NAME = 'dockerhub.hi.inet/dummy-netapp-testing/robot-test-image'
    }

    stages{

        stage("Checkout code"){
            steps{
                checkout([$class: 'GitSCM',
                    branches: [[name: 'main']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "$NetApp_repo"]], 
                    gitTool: 'Default',
                    submoduleCfg: [],
                    userRemoteConfigs: [[url: "https://github.com/EVOLVED-5G/${NetApp_repo}.git"]]
                ])
                // checkout([$class: 'GitSCM',
                //     branches: [[name: 'develop']],
                //     doGenerateSubmoduleConfigurations: false,
                //     extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "capif-services"]], 
                //     gitTool: 'Default',
                //     submoduleCfg: [],
                //     userRemoteConfigs: [[url: 'https://github.com/EVOLVED-5G/CAPIF_API_Services.git']]
                // ])
                // checkout([$class: 'GitSCM',
                //     branches: [[name: 'main']],
                //     doGenerateSubmoduleConfigurations: false,
                //     extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "nef-services"]], 
                //     gitTool: 'Default',
                //     submoduleCfg: [],
                //     userRemoteConfigs: [[url: 'https://github.com/EVOLVED-5G/NEF_emulator.git']]
                // ])
                sh '''
                    ls -la
                '''
            }
        }

        stage("Set Netapp, Nef & Capif locally."){
            stages{
                
                stage("Setup environment"){
                    steps {
                        // dir ("./capif-services") {
                        //     sh 'ls && cd services && ./run.sh'
                        // }
                        // dir ("./nef-services") {
                        //     sh 'ls && make prepare-dev-env && make build && make up && make db-init'
                        // }
                        // dir ("./$NetApp_repo") {
                        //     sh 'ls && ./run.sh'
                        // }
                        dir ("${ROOT_DIRECTORY}") {
                            // sh """
                            //     touch .env && echo "ROBOT_IMAGE_NAME=${ROBOT_DOCKER_IMAGE_NAME}:${ROBOT_DOCKER_IMAGE_VERSION}" > .env && echo "NETAPP_NAME=${NetApp_repo}"
                            // """
                            // sh """
                            //     docker-compose up -d --build
                            //     docker ps -a
                            // """
                            // -e NEF_SERVICES_ENDPOINT=${NEF_SERVICES_ENDPOINT}
                            // -e CAPIF_SERVICES_ENDPOINT=${CAPIF_SERVICES_ENDPOINT}
                            sh """
                                docker build -t netapp_robot_image ${ROBOT_DOCKER_IMAGE_NAME}:${ROBOT_DOCKER_IMAGE_VERSION}
                                docker run -d -t --name netapp_robot \
                                -e NEF_SERVICES_ENDPOINT=${NEF_HOSTNAME} \
                                -e CAPIF_SERVICES_ENDPOINT=${CAPIF_HOSTNAME} \
                                -v ${ROOT_DIRECTORY}/${NetApp_repo}/capif_callback_server:/opt/robot-tests/capif-callback \
                                -v ${ROOT_DIRECTORY}/${NetApp_repo}/nef_callback_server:/opt/robot-tests/nef-callback \
                                -v ${ROOT_DIRECTORY}/${NetApp_repo}/pythonnetapp:/opt/robot-tests/pythonnetapp \
                                -v ./tests:/opt/robot-tests/tests/ \
                                -v ./libraries:/opt/robot-tests/libraries/ \
                                -v ./resources:/opt/robot-tests/resources/ \
                                -v ./results:/opt/robot-tests/results/ netapp_robot_image 
                            """
                        }
                    }
                }
                stage("Run test cases."){
                    steps{
                        sh """
                            docker exec -t netapp_robot bash \
                            -c "robot ./tests/tests/capif_invoker_tests/dummy-tests.robot; \
                                robot ./tests/tests/capif_discover_services/discover_services_tests.robot; \
                                robot ./tests/tests/capif_publish_services/publish_services_tests.robot;"
                        """
                        // robot ./tests/tests/nef_monitoring_events/nef_monitoring_events_tests.robot;
                        // robot ./tests/tests/nef_as_sessions/nef_as_sessions_tests.robot;
                        // robot ./tests/tests/callbacks/callback_tests.robot.robot;
                    }
                }

            }

        }

    }

    post{
        always{
            script {
                dir ("${env.ROOT_DIRECTORY}") {
                    // echo 'Shutdown all services'
                    // sh 'docker-compose down -v'
                    sh 'docker kill netapp_robot && docker rm netapp_robot && docker rmi netapp_robot_image'
                }
                dir ("./$NetApp_repo") {
                    sh './cleanup_docker_containers.sh'
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