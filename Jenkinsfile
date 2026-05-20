pipeline {

    agent any

    stages {

        stage('Clone') {
            steps {
                echo 'Repository cloned'
            }
        }

        stage('Build Backend') {
            steps {
                sh 'echo Building Backend'
            }
        }

        stage('Docker Build') {
            steps {
                sh 'echo Docker Build'
            }
        }

        stage('Docker Push') {
            steps {
                sh 'echo Docker Push'
            }
        }

        stage('Deploy Kubernetes') {
            steps {
                sh 'echo Deploying to Kubernetes'
            }
        }
    }
}