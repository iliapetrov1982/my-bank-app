pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = credentials('docker-registry-url')
        DOCKER_CREDENTIALS = credentials('docker-registry-credentials')
        HELM_RELEASE = 'my-bank'
        HELM_CHART = './helm/my-bank'
    }

    options {
        timestamps()
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build -x test'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Docker Build & Push') {
            parallel {
                stage('accounts') {
                    steps {
                        buildAndPush('accounts')
                    }
                }
                stage('cash') {
                    steps {
                        buildAndPush('cash')
                    }
                }
                stage('transfer') {
                    steps {
                        buildAndPush('transfer')
                    }
                }
                stage('notifications') {
                    steps {
                        buildAndPush('notifications')
                    }
                }
                stage('gateway') {
                    steps {
                        buildAndPush('gateway')
                    }
                }
                stage('front-ui') {
                    steps {
                        buildAndPush('front-ui')
                    }
                }
            }
        }

        stage('Helm Lint') {
            steps {
                sh "helm lint ${HELM_CHART}"
            }
        }

        stage('Deploy') {
            steps {
                sh "helm upgrade --install ${HELM_RELEASE} ${HELM_CHART} --set global.image.registry=${DOCKER_REGISTRY} --set global.image.tag=${env.BUILD_NUMBER}"
            }
        }

        stage('Helm Test') {
            steps {
                sh "helm test ${HELM_RELEASE}"
            }
        }
    }

    post {
        failure {
            echo "Pipeline failed: ${env.BUILD_URL}"
        }
        success {
            echo "Pipeline succeeded: ${env.BUILD_URL}"
        }
    }
}

void buildAndPush(String service) {
    sh """
        docker build -t ${DOCKER_REGISTRY}/my-bank-${service}:${env.BUILD_NUMBER} ./${service}
        docker push ${DOCKER_REGISTRY}/my-bank-${service}:${env.BUILD_NUMBER}
    """
}