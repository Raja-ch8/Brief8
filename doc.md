# **Brief 8 - Complete CI/CD**

### *Contexte du projet*

- Créer un pipeline qui automatise l'image Docker à chaque mise à jour.
- déployer un pipeline d'intégration et de déploiement continu pour l'application **AZURE VOTING** et sa base de données REDIS.

### *Méthode*

**1. J'ai créé le mv en utilisant les scripts du brief précédent (Brief7) : main.tf, variable.tf et providers.tf.**

Après avoir créé la machine virtuelle j'ai installé java et jenkins (j'ai eu du mal à installer jenkins car la taille de Debian il été pas suffisant du coup j'ai modifier la taille de Debian à *"Standard_A2_v2"* ).
Sur la mv j'ai installer:

* **Installé Azure Cli**
  
- Obtenez les packages nécessaires pour le processus d’installation :
  
```consol 

sudo apt-get update
sudo apt-get install ca-certificates curl apt-transport-https lsb-release gnupg

```

- Téléchargez et installez la clé de signature Microsoft :

```cosnol
sudo mkdir -p /etc/apt/keyrings
curl -sLS https://packages.microsoft.com/keys/microsoft.asc |
    gpg --dearmor |
    sudo tee /etc/apt/keyrings/microsoft.gpg > /dev/null
sudo chmod go+r /etc/apt/keyrings/microsoft.gpg

```

- Ajoutez le référentiel de logiciels Azure CLI :

```consol 

AZ_REPO=$(lsb_release -cs)
echo "deb [arch=`dpkg --print-architecture` signed-by=/etc/apt/keyrings/microsoft.gpg] https://packages.microsoft.com/repos/azure-cli/ $AZ_REPO main" |
sudo tee /etc/apt/sources.list.d/azure-cli.list

```

- Mettez à jour les informations concernant le référentiel, puis installez le package azure-cli :
  
```consol 
sudo apt-get update
sudo apt-get install azure-cli

```

* **Docker** : https://www.it-connect.fr/installation-pas-a-pas-de-docker-sur-debian-11/


* **jq** 
```consol 
sudo apt install jq
```

* **kubectl** 

```consol 
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
curl -LO "https://dl.k8s.io/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

```

* **Git**

```consol 
sudo apt-add-repository ppa:git-core/ppa
sudo apt-get update
sudo apt-get install git
```

Après avoir pu accéder à Jenkins, j'ai installé des plugin: 

-> Administer Jenkins -> Gestion des plugins -> Available plugins

- Kubernetes Credential
- kubernetes Cli
- Kubernetes 
- Pipeline 
- Workspace Cleanup
- Docker API
- Docker commons
- Docker pipeline
- Docker plugin 
- Docker build-stcp

- **Installare Kubectl**      

```consol 

      curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
      curl -LO "https://dl.k8s.io/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
      echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check
      sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

```

1. Creation groupe de resources: 
    
    ```consol
      -  az group create --location francecentral --resource-group Brief8-Raja
    ```
2. Creare un cluster Aks 

    ```consol
      - az aks create -g Brief8-Raja -n AKSCluster --generate-ssh-key --node-count 1 --enable-managed-identity -a ingress-appgw --appgw-name myApplicationGateway --appgw-subnet-cidr "10.225.0.0/16"   
    ```

    ```consol
      - az aks get-credentials --name AKSCluster --resource-group Brief8-Raja 
      ```
  
  (installer aussi sur la mv, permet de faire une lien entre jenkins et aks)

**2. J'ai créé namespace.**

- Credential AKS -> permet de créer un lien entre les clusters Aks et le pipeline jenkins.
- kubectl create namespace qal
- kubectl create namespace prod.

**3. Creation de la pipeline**

    - Récupérez le fichier kube.config et collez-le sur le pipeline.**
    
        ```consol 
        ls -a 
        cd .kube
        cat config
        ```

**4.Créer un compte sur Docker Hub**

-Ajouter un gestionnaire d'informations d'identification aux plugins de pipeline

- **Installare la permission de sudo per jenkins**

```consol
      sudo su
      visudo -f /etc/sudoers 
      ajouter: **JENKINS ALL= NOPASSWD:ALL**
```

- **Installer kubens**

    ```consol
    sudo git clone https://github.com/ahmetb/kubectx /opt/kubectx
    sudo ln -s /opt/kubectx/kubectx /usr/local/bin/kubectx
    sudo ln -s /opt/kubectx/kubens /usr/local/bin/  kubens

    ```
**5. Pipeline Jenkins**

- *Probleme*
  1. Le problème que j'ai rencontré était que je ne pouvais pas utiliser cette commande sur le pipeline, j'ai donc demandé à accéder directement au terminal mv de jenkins en faisant:

        ```consol 
           sudo -iu jenkins
           sudo docker login 
        ```         

  2. Problème : lorsque j'ajoute la partie des identifiants aks au pipeline j'obtiens un message d'erreur le problème c'est que j'ai fait un couple et coller directement mais cela fait que le fichier de test ajoute des caractères qui ne sont pas visibles donc il est conseillé de travailler dans *BASH* et exécutez la commande suivante :
    SHIFT + COMMAND + G (faites le sur le texte mais sur rajachoukri puis mettez le chemin de kube puis .kube/config).

  
  3. Problème : Après avoir créé le référentiel Brief7 cloné, j'ai eu une erreur, il m'a dit que le beckend était défectueux. Je suis donc allé sur AKS ou LENS et j'ai remarqué qu'il y avait un problème lors du vote. L'erreur était que j'avais oublié de modifier le référentiel DockerHub sur mon pipeline.
   
  4. *TEST DE CHARGE* 
   
   - Installer *Parallel* **SUR LA VM JENKINS**
   - Récupérer DNS Gandi: 
   - Récupération du script du bouton de charge réalisé dans Brief4.
   
   - Pour changer les votes pour ne pas toujours laisser les mêmes choix entre *Windows* et *Linux* allez sur main.py et changez les choix de vote et la version car j'ai eu un problème pour le faire fonctionner à la version 1.0.110 j'ai dû changer la version à 1.0.111. 
   
 **SCRIPT PIPELINE**   
       
```consol 

pipeline {
    agent any
    stages {
        stage('Clone Repository') {
            steps {
                sh('''
                git clone https://github.com/simplon-choukriraja/Brief8-Raja.git
                ''')
            }
        }
        
        
        stage('Build Image') {
            steps {
                sh ('''
                cd Brief8-Raja
                sudo docker build -t vote-app .
                ''')
            }
        }
        
        
        stage('Push') {
            steps {
                sh ('''
                PATCH=$(cat Brief8-Raja/azure-vote/main.py | grep "ver = ".*"" | grep -oE "[0-9]+\\.[0-9]+\\.[0-9]+")
                sudo docker tag vote-app raja8/vote-app:\044PATCH
                sudo docker push raja8/vote-app:\044PATCH
    
                ''')
            }
        }
        
        stage('Clone repository app-vote') {
            steps {
                withKubeConfig([credentialsId: 'aks']) {
                    sh('''
                    git clone https://github.com/simplon-choukriraja/brief7-votinapp.git app
                    TAG=\044(curl -sSf https://registry.hub.docker.com/v2/repositories/raja8/vote-app/tags |jq '."results"[0]["name"]'| tr -d '"')
                    sed -i "s/TAG/\044{TAG}/" ./app/vote.yml
                    kubens qal
                    kubectl apply -f ./app 
                    ''')
                }
            }
        }
        
        stage('Test de charge'){
            steps{
                sh('''
                seq 250 | parallel --max-args 0  --jobs 10 "curl -k -iF 'vote=Pizza' http://vote.simplon-raja.space"
                ''')
            }
        }
    }


    post {
        always {
            // Nettoyage de l'espace de travail Jenkins
            step([$class: 'WsCleanup'])
        }
    }
}

```

**Application Vote**

![](https://i.imgur.com/Gy4BHSH.png)
