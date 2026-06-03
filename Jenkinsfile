pipeline {

    agent any

    environment {
        DOCKER_USERNAME = "nitinr1103"
        IMAGE_NAME = "managio-backend"
    }

    stages {

        stage('Clone') {
            steps {
                echo 'Cloning Repository'
            }
        }

        stage('Build Backend') {
            steps {
                sh 'cd managio_backend && chmod +x mvnw && ./mvnw clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $DOCKER_USERNAME/$IMAGE_NAME:latest ./managio_backend'
            }
        }

        stage('Docker Push') {
    steps {
        withCredentials([
            string(credentialsId: 'dockerhub-token', variable: 'DOCKER_TOKEN')
        ]) {
            sh 'docker login -u $DOCKER_USERNAME -p $DOCKER_TOKEN'
            sh 'docker push $DOCKER_USERNAME/$IMAGE_NAME:latest'
        }
    }
}

        stage('Deploy Kubernetes') {
            steps {
                sh 'kubectl rollout restart deployment backend'
            }
        }
    }
}