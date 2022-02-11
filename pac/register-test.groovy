pipeline {
    agent { node { label 'evol5-slave' }  }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '30', artifactDaysToKeepStr: '14', artifactNumToKeepStr: '30'))
        ansiColor('xterm')
    }
    parameters {
        // string(name: 'BRANCH_NAME', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'NGINX_HOSTNAME', defaultValue: 'nginx-evolved5g.apps-dev.hi.inet', description: 'nginx hostname')
        string(name: 'ROBOT_DOCKER_IMAGE_VERSION', defaultValue: '2.0', description: 'Robot Docker image version')
        string(name: 'ROBOT_TEST_OPTIONS', defaultValue: '', description: 'Options to set in test to robot testing. --variable <key>:<value>, --include <tag>, --exclude <tag>')
        // string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'DUMMY_NETAPP_HOSTNAME', defaultValue: 'dummy-netapp-evolved5g.apps-dev.hi.inet', description: 'netapp hostname')
    }
    environment {
        // BRANCH_NAME = "${params.BRANCH_NAME}"
        CAPIF_SERVICES_DIRECTORY = "${WORKSPACE}/services"
        ROBOT_TESTS_DIRECTORY = "${WORKSPACE}/tests"
        ROBOT_RESULTS_DIRECTORY = "${WORKSPACE}/results"
        NGINX_HOSTNAME = "${params.NGINX_HOSTNAME}"
        ROBOT_VERSION = 'latest'
        ROBOT_IMAGE_NAME = 'dockerhub.hi.inet/dummy-netapp-testing/robot-test-image'
        // GIT_BRANCH="${params.GIT_BRANCH}"
        DUMMY_NETAPP_HOSTNAME="${params.DUMMY_NETAPP_HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        OPENSHIFT_URL= 'https://openshift-epg.hi.inet:443'
    }
    stages{
        stage('Login openshift') {
            steps {
                withCredentials([string(credentialsId: '18e7aeb8-5552-4cbb-bf66-2402ca6772de', variable: 'TOKEN')]) {
                    sh '''
                        export KUBECONFIG="./kubeconfig"
                        oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL
                    '''
                    readFile('kubeconfig')
                }
            }
        }

        stage("Check Pods"){
            steps{
                sh """
                    oc get pods
                """
            }
        }

        // stage("Build container"){
        //     steps{
        //         sh """
        //             oc create -f deploymentConfig.yaml -ntest
        //         """
        //     }
        // }

        // stage("Run tests"){
        //     steps{
        //         sh """
        //             oc cp ../tests robot-framework:/tests
        //             oc -ntest exec -it robot-deployment -- /bin/bash -c "robot ./tests/feature/user_register.robot"
        //         """
        //     }
        // }
    }
    post {
        always {

            script {
                /* Manually clean up /keys due to permissions failure */
                echo 'Robot test executed'
                echo ' clean dockerhub credentials'
                sh 'sudo rm -f ${HOME}/.docker/config.json'
            }

            publishHTML([allowMissing: true,
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: 'results',
                    reportFiles: 'report.html',
                    reportName: 'Robot Framework Tests Report',
                    reportTitles: '',
                    includes:'**/*'])
            junit allowEmptyResults: true, testResults: 'results/xunit.xml'

            script {
                dir ("${env.WORKSPACE}") {
                    sh "sudo rm -rf ${env.ROBOT_TESTS_DIRECTORY}"
                    sh "sudo rm -rf ${env.CAPIF_SERVICES_DIRECTORY}"
                }
            }
        }
        cleanup {
            /* clean up our workspace */
            deleteDir()
            /* clean up tmp directory */
            dir("${env.workspace}@tmp") {
                deleteDir()
            }
            /* clean up script directory */
            dir("${env.workspace}@script") {
                deleteDir()
            }
        }
    }
}