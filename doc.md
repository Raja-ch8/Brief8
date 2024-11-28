# **Brief 8 - Mettre en place un pipeline d’intégration et de déploiement continu (CI/CD) pour l’application Azure Voting App**

### **Contexte du projet**

L'objectif de ce projet est de mettre en place un pipeline automatisé pour la gestion des images Docker, garantissant leur mise à jour continue à chaque modification de l'application. Il s'agit également de déployer un pipeline d'intégration et de déploiement continus (CI/CD) pour l'application Azure Voting et sa base de données Redis. 

### *Installation et configuration des outils nécessaires*

**1. J'ai créé la machine virtuelle en utilisant les scripts fournis dans le brief précédent (Brief 7), notamment les fichiers main.tf, variables.tf, et providers.tf.**

Une fois la machine virtuelle déployée, j'ai installé Java et Jenkins. Cependant, j'ai rencontré un problème lors de l'installation de Jenkins en raison de la taille initiale insuffisante de l'instance Debian. Pour résoudre cela, j'ai modifié la taille de l'instance en passant à Standard_A2_v2, ce qui a permis d'allouer les ressources nécessaires.

Après cette configuration, Java et Jenkins ont été correctement installés et sont maintenant fonctionnels sur la machine virtuelle.

**Installation Azure CLI**

Pour installer Azure CLI, procédez comme suit :

1 - Mettez à jour et installez les packages requis pour l’installation :
- Obtenez les packages nécessaires pour le processus d’installation :
  
```bash
sudo apt-get update
sudo apt-get install ca-certificates curl apt-transport-https lsb-release gnupg

```

2 - Téléchargez et installez la clé de signature Microsoft :

```bash
sudo mkdir -p /etc/apt/keyrings
curl -sLS https://packages.microsoft.com/keys/microsoft.asc |
    gpg --dearmor |
    sudo tee /etc/apt/keyrings/microsoft.gpg > /dev/null
sudo chmod go+r /etc/apt/keyrings/microsoft.gpg

```

3 - Ajoutez le référentiel de logiciels Azure CLI :

```bash 

AZ_REPO=$(lsb_release -cs)
echo "deb [arch=`dpkg --print-architecture` signed-by=/etc/apt/keyrings/microsoft.gpg] https://packages.microsoft.com/repos/azure-cli/ $AZ_REPO main" |
sudo tee /etc/apt/sources.list.d/azure-cli.list

```

4 - Mettez à jour et installez Azure CLI :
  
```bash
sudo apt-get update
sudo apt-get install azure-cli

```
### **Installation de Docker**


* **Docker** : https://www.it-connect.fr/installation-pas-a-pas-de-docker-sur-debian-11/

### **Installation autres outils**

- **jq** 

**jq** est un outil en ligne de commande léger et flexible conçu pour manipuler, traiter et interroger des données au format JSON.

```bash
sudo apt install jq
```

- **Installation kubectl** 

```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
curl -LO "https://dl.k8s.io/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

- **Installation Git**

```bash
sudo apt-add-repository ppa:git-core/ppa
sudo apt-get update
sudo apt-get install git
```
- **Installation Kubens**

```bash
sudo git clone https://github.com/ahmetb/kubectx /opt/kubectx
sudo ln -s /opt/kubectx/kubectx /usr/local/bin/kubectx
sudo ln -s /opt/kubectx/kubens /usr/local/bin/kubens
```

### **Configuration et création des ressources Azure**

1. **Création d’un groupe de ressources :**
   
```bash
az group create --location francecentral --resource-group Brief8-Raja
```
2. Création d’un cluster AKS :
   
```bash
az aks create -g Brief8-Raja -n AKSCluster --generate-ssh-key --node-count 1 --enable-managed-identity -a ingress-appgw --appgw-name myApplicationGateway --appgw-subnet-cidr "10.225.0.0/16"
az aks get-credentials --name AKSCluster --resource-group Brief8-Raja

```
3. **Création de namespaces dans Kubernetes :**

```bash
kubectl create namespace qal
kubectl create namespace prod
```

### **Installation et configuration de Jenkins**

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

* J’ai également configuré Jenkins pour se connecter à Docker et au cluster AKS en modifiant le fichier sudoers pour accorder les droits nécessaires :

```consol
sudo visudo -f /etc/sudoers
```
- Ajoutez la ligne suivante :

```bash
jenkins ALL=(ALL) NOPASSWD:ALL
```

### **Configuration des namespaces Kubernetes et des credentials AKS**

1. **Création des namespaces :**

Les namespaces sont utilisés pour organiser et isoler les ressources dans Kubernetes. J’ai créé deux namespaces pour gérer les environnements :

```bash
kubectl create namespace qal
kubectl create namespace prod
```

2. **Configuration des credentials AKS dans Jenkins :**
Pour établir un lien entre le cluster AKS et le pipeline Jenkins, il est nécessaire de configurer les credentials Kubernetes. Voici les étapes :

- Récupérez le fichier kube.config depuis la machine où le cluster est configuré :

```bash
ls -a
cd .kube
cat config
```
- Copiez son contenu, puis collez-le dans Jenkins en tant que credential spécifique au cluster AKS. Cela permettra à Jenkins d’interagir avec le cluster Kubernetes lors du déploiement.
        
### **Création et test du pipeline Jenkins**

- *Script du pipeline*

```java
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

```

### **Résolution des problèmes rencontrés**

* **Connexion à Docker depuis Jenkins :**
Pour résoudre les problèmes de permissions, j’ai exécuté :

```bash
sudo -iu jenkins
sudo docker login
```

* **Correction des identifiants AKS :**
Évitez le copier-coller direct depuis un éditeur qui pourrait ajouter des caractères invisibles. Utilisez plutôt Bash pour éditer le fichier kubeconfig :

```bash
cd ~/.kube
cat config

```

* **Mise à jour du référentiel Docker Hub :**
J’ai corrigé le pipeline pour inclure l’image Docker mise à jour, en modifiant le fichier YAML avec la bonne version.

----

Ce pipeline met en œuvre les meilleures pratiques CI/CD tout en automatisant le déploiement de l’application Azure Voting avec des tests de charge pour valider la scalabilité et la performance.
**Application Vote**

![](https://i.imgur.com/Gy4BHSH.png)
