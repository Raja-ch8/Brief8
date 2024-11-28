pipeline {
    agent any
    stages {
        stage('Clone Repository') {
            steps {
                sh 'git clone https://github.com/simplon-choukriraja/Brief8-Raja.git'
            }
        }
        
        stage('Build Image') {
            steps {
                sh '''
                cd Brief8-Raja
                sudo docker build -t vote-app .
                '''
            }
        }
        
        stage('Push') {
            steps {
                sh '''
                PATCH=$(cat Brief8-Raja/azure-vote/main.py | grep "ver = ".*"" | grep -oE "[0-9]+\\.[0-9]+\\.[0-9]+")
                sudo docker tag vote-app raja8/vote-app:$PATCH
                sudo docker push raja8/vote-app:$PATCH
                '''
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                withKubeConfig([credentialsId: 'aks']) {
                    sh '''
                    git clone https://github.com/simplon-choukriraja/brief7-votinapp.git app
                    TAG=$(curl -sSf https://registry.hub.docker.com/v2/repositories/raja8/vote-app/tags | jq -r '."results"[0]["name"]')
                    sed -i "s/TAG/${TAG}/" ./app/vote.yml
                    kubens qal
                    kubectl apply -f ./app
                    '''
                }
            }
        }
        
        stage('Load Test') {
            steps {
                sh '''
                seq 250 | parallel --max-args 0 --jobs 10 "curl -k -iF 'vote=Pizza' http://vote.simplon-raja.space"
                '''
            }
        }
    }
    post {
        always {
            step([$class: 'WsCleanup'])
        }
    }
}
