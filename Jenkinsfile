pipeline {
    agent any

    environment {
        IMAGE_NAME = "ms-anomaly-detector"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh './gradlew clean test'
            }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Build Jar') {
            steps {
                sh './gradlew bootJar'
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} -t ${IMAGE_NAME}:latest ."
            }
        }
    }

    post {
        failure {
            echo "Build #${BUILD_NUMBER} failed"
        }
        success {
            echo "Build #${BUILD_NUMBER} succeeded. Image: ${IMAGE_NAME}:${BUILD_NUMBER}"
        }
    }
}