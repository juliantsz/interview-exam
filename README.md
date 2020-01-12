# Reto Devops

### Imágen Docker Generada [java-tomcat-maven-example](https://hub.docker.com/repository/docker/crafterox4/java-tomcat-maven-example)

### Setup de Jenkins

Para tener un servidor de Jenkins en el cual trabajar opte por la opción de contenedor con Docker corriendo en mi local, con la portabilidad de Docker este contenedor de Jenkins puede ser levantado en la nube o on-premise. Ejecutando este archivo [yaml](https://github.com/juliantsz/jenkins-shared-library/blob/master/src/jenkins-compose.yml) con `docker-compose`. En el explorador colocamos la dirección `ip` del servidor (127.0.0.1 para localhost) seguido del puerto `8080`, `127.0.0.1:8080`. De esta manera tenemos Jenkins.

##### plugins utilizados
[pipeline-utility-steps](https://plugins.jenkins.io/pipeline-utility-steps) Este plugin nos permite leer y escribir archivos como `yaml`, `json`, `properties`, `pom.xml`, etc. En este proyecto fue utilizado para leer el archivo `pom.xml` dentro del repositorio para poder obtener el `artifactId` y `version` del proyecto. De esta manera las imágenes Docker son generadas dinámicamente leyendo este archivo.

[SSH Pipeline Steps](https://plugins.jenkins.io/ssh-steps) Este plugin nos permite copiar, obtener, eliminar archivos y ejecutar comandos de servidores remotos utilizando credenciales definidas en las credenciales de Jenkins. Todo esto de manera segura y sin quemas credenciales en el codigo. Las credenciales al estar en un lugar centralizado, se pueden actualizar facilmente sin tener que modificar el código.

[Config File Provider Plugin](https://wiki.jenkins.io/display/JENKINS/Config+File+Provider+Plugin) Nos permite inyectar archivos de configuración en tiempo de ejecución del pipeline. Por ejemplo `settings.xml`. De esta manera se puede modificar sin tener que ingresar a un servidor o contenedor.

##### jenkins-shared-libraries
Jenkins puede ser ejecutada de muchas maneras. Una de estas es con un `Jenkinsfile` sin embargo el problema es que este archivo es guardado en el repositorio donde se encuentra el código de los desarrolladores, corriendo el riesgo de ser modificado o eliminado por alguien distinto al DevOps del proyecto. Con [jenkins-shared-libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) se define un repositorio donde se versiona el código utilizado por el DevOps. De esta manera se tiene por un lado el código de los desarrolladores y el código del DevOps separado, evitando conflictos. Para su uso es necesario cierta configuración y estructura del repositorio.

1. en las configuraciones de Jenkins buscamos la sección llamada `Global Pipeline Libraries` y agregamos el nombre de la librearia compartida, rama default que queremos usar (por lo general `master`), url del repositorio, credenciales y que tipo de repositorio es. 
![alt text](https://github.com/juliantsz/images/blob/master/shared-library.png)

2. El repositorio debe seguir una estructura especifica
```
(root)
+- src                     # Groovy source files
|
+- vars
|   +- ci.groovy          # Para variables globales
+- resources              # resource files como .sh, .yaml, .tf etc
|   
```
3. Se crea un `pipeline` en Jenkins, en la última llamada `pipeline` escribimos lo siguiente
```
@Library('jenkins-library') _
ciMaven()
```
`@Library` llamamos la libreria configurada previamante, tener en cuenta que esta usa la rama definida. En este caso `master`. Luego se llama al archivo groovy, en este caso `ciMaven()`. De esta manera se tiene un repositorio exlusivo para integración y despliegue continuo, sin tener `Jenkinsfiles` dentro del repositorio de la aplicación.


##### Ejecución del Pipeline

- Empezamos el pipeline definiendo en que nodo correr y su workspace
```
agent {
    node {
        label "master"
        customWorkspace "/var/jenkins_home/workspace/${env.BUILD_TAG}"
    }
}
```
- Agregamos una instalación de maven en tiempo de ejecución. De esta manera evitamos que entrar al maestro o esclavo e instalar maven en este caso. Sino que Jenkins hace esto por nosotros instalando una `tool` definida en `configuration/managed tools`. Otra ventaja es cambiar de versión rapidamente simplemente cambiando el `tool`
```
tools {
    maven 'maven-3.6.3'
}
options {
    timestamps() 
}
```
- La primera etapa es clonar el repositorio
```
stage('Clone Repo') {
    steps {
        script {
            sh 'printenv | sort'
            ciUtils.gitCheckout(
                "master",//branch
                "github",//credentials
                "https://github.com/daticahealth/java-tomcat-maven-example.git"//url
            )
            POM = readMavenPom file: 'pom.xml'
        }
    }
}
```
`ciUtils.gitCheckout()` es una función definida dentro del archivo `vars/ciUtils`. Esto ayuda a la reutilización de código simplemente llamando la función y pasando los parámetros necesarios.

``` ciUtils.groovy
def gitCheckout(String branch, String credentials, String url){
    checkout([
        $class: 'GitSCM', 
        branches: [[name: "${branch}"]], 
        doGenerateSubmoduleConfigurations: false, 
        extensions: [[$class: 'CleanCheckout']], 
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: "${credentials}", url: "${url}"]]
    ])
}
```
[checkout](https://wiki.jenkins.io/display/JENKINS/Git+Plugin) lo podemos encontrar en el plugin de Git

- Etapas maven
```
stage('Maven Compile') {
    steps {
        script {
            withMaven(
                mavenSettingsConfig: 'maven-settings') {
                sh "mvn package"
            }
        }
    }
}
stage('Maven test-compile') {
    steps {
        script {
            sh "mvn test"
        }
    }
}
stage('Maven Scan') {
    steps {
        script {
            sh "mvn sonar:sonar"
        }
    }
}
```
con `mvn package` generamos los artefactos. `mvn test` realizamos pruebas unitarias y definiendo un `goal` en el `settings.xml` podemos ejecutar `mvn sonar:sonar` y ver mas detalles en cuanto a seguridad y calidad de código y definir quality gates
![alt text](https://github.com/juliantsz/images/blob/master/sonar.png)


![alt text](https://github.com/juliantsz/images/blob/master/sonar-overview.png)

`settings.xml` definido en managed files
```
<settings>
    <pluginGroups>
        <pluginGroup>org.sonarsource.scanner.maven</pluginGroup>
    </pluginGroups>
    <profiles>
        <profile>
            <id>sonar</id>
            <properties>
                <sonar.host.url>
                  http://127.0.0.1:9000
                </sonar.host.url>
                <sonar.login>
                  0214e4e78d5bd10c0202c9e9bbea3f4118bfd603
              </sonar.login>
            </properties>
        </profile>
     </profiles>
    <activeProfiles>
      <activeProfile>sonar</activeProfile>
    </activeProfiles>
</settings>
```
Para evitar que colocar las credenciales de acceso a sonar, generamos un token dentro de sonar. De esta manera en la llave `<sonar.login>` ingresamos este token y así poder publicar los proyectos escaneados en sonar.

Para este proyecto sonar fue ejecutado en un contenedor de Docker

- Construcción de imágen Docker

```
stage('Build Docker Image') {
    steps {
        script {
            ciUtils.buildImage(
                "ec2user",//credentials
                "${env.ec2ip}",//server
                "${POM.artifactId}",//artifactId
                "${POM.version}"//version
            )
        }
    }
}
```
En esta etapa se ejecuta la función `buildImage` dentro del archivo `ciUtils.groovy` con los respectivos parametros.

``` buildImage()

def buildImage(String credentials, String server, String artifactId, String version) {
    writeFile file: 'Dockerfile', text:libraryResource("docker/Dockerfile")
    writeFile file: 'buildImage.sh', text:libraryResource("bash/buildImage.sh")
    def remote = [:]
    remote.name = "${server}"
    remote.host = "${server}"
    remote.allowAnyHosts = true
    withCredentials([usernamePassword(credentialsId: "${credentials}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        remote.user = USERNAME
        remote.password = PASSWORD
        sshPut remote: remote, from: "${WORKSPACE}/Dockerfile", into: '/home/ec2-user/'
        sshPut remote: remote, from: "${WORKSPACE}/buildImage.sh", into: '/home/ec2-user/'
        sshPut remote: remote, from: "${WORKSPACE}/target/${artifactId}.war", into: '/home/ec2-user/app.war'
        sshPut remote: remote, from: "${WORKSPACE}/target/dependency/webapp-runner.jar", into: '/home/ec2-user/app.jar'
        sshCommand remote: remote, command: "cd /home/ec2-user/; chmod +x buildImage.sh; ./buildImage.sh ${env.dockerhub_user}${artifactId} ${version}"
        sshCommand remote: remote, command: "cd /home/ec2-user/; rm *"
    }
}
```
Primero generamos un archivo [Dockerfile](https://github.com/juliantsz/jenkins-shared-library/blob/master/resources/docker/Dockerfile) y un shell script [buildImage.sh](https://github.com/juliantsz/jenkins-shared-library/blob/master/resources/bash/buildImage.sh) tomados de la carpeta `resources` del jenkins-shared-library al `workspace` del pipeline. Luego haciendo uso del plugin [SSH Pipeline Steps](https://plugins.jenkins.io/ssh-steps) nos conectamos a un servidor para poder construir la imagen Docker. Este servidor es una instancia ec2 con docker instalado. 

- La dirección ip del servidor esta guardada como variable de entorno en Jenkins, de esta manera si dicha dirección cambia solo basta con cambiarla en un solo lugar sin modificar código.

- `withCredentials` nos permite mapear el usuario y contraseña tomadas de las credenciales de Jenkins de manera segura

- Una vez conectados al servidor procedemos primero a copiar el `Dockerfile`, `buildImage.sh`, `war generado` y `webapp-runner.jar` a la ruta `home/ec2-user/` dentro del servidor.

- Copiado todo esto con `sshCommand` ejecutamos comandos dentro del servidor. Para simplicidad y no tener que ejecutar varios comandos, procedemos a ejecutar el shell. Este shell hará todo el trabajo por nosotros. Como construir la imágen, subir la imágen a [docker hub](https://hub.docker.com/repository/docker/crafterox4/java-tomcat-maven-example) y eliminar la imágen del servidor. 

- Por último, con otro `sshCommand` eliminamos todo lo copiado al servidor. De esta manera estará limpio para la siguiente ejecución.


- Despliegue a un clúster de Kubernetes
```
stage('Deploy Pod') {
    steps {
        script {
            ciUtils.deployPod(
                "cloud_user",//credentials
                "${env.k8_server}"//server
            )
        }
    }
}
```

Muy similiar a la construcción de la imágen Doccker, el despliegue al clúster de Kubernetes hace uso de una función llamada `deployPod`

``` deployPod()
def deployPod(String credentials, String server) {
    writeFile file: 'pod.yml', text:libraryResource("pod/pod.yml")
    writeFile file: 'service.yml', text:libraryResource("pod/service.yml")
    def remote = [:]
    remote.name = "${server}"
    remote.host = "${server}"
    remote.allowAnyHosts = true
    withCredentials([usernamePassword(credentialsId: "${credentials}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        remote.user = USERNAME
        remote.password = PASSWORD
        sshPut remote: remote, from: "${WORKSPACE}/pod.yml", into: '/home/cloud_user/'
        sshPut remote: remote, from: "${WORKSPACE}/service.yml", into: '/home/cloud_user/'
        sshCommand remote: remote, command: "cd /home/cloud_user/; kubectl apply -f pod.yml"
        sshCommand remote: remote, command: "cd /home/cloud_user/; kubectl apply -f service.yml"
        sshCommand remote: remote, command: "cd /home/cloud_user/; rm pod.yml service.yml"
    }
}
```

- Primero generamos dos archivo `yaml` tomados de la carpeta `resources`. [pod.yml](https://github.com/juliantsz/jenkins-shared-library/blob/master/resources/pod/pod.yml) encargado de generar un despliegue con dos replicas usando la imágen subida a [docker hub](https://hub.docker.com/repository/docker/crafterox4/java-tomcat-maven-example) y [service.yml](https://github.com/juliantsz/jenkins-shared-library/blob/master/resources/pod/service.yml) para poder acceder a ella desde afuera del clúster. 

- Copiamos estos dos `yaml` al servidor con `sshPut` en la ruta `home/cloud_user/` 

- Con `sshCommand` ejecutamos estos dos `yaml` dentro del servido. `kubectl apply -f`.

- Eliminar estos dos `yaml` con un `sshCommand` 

#### Verificar

Para verificar despliegue. En un explorador ingresar la dirección ip del servidor o dns, seguido del puerto definido en el `service.yml` 

[webapp](https://github.com/juliantsz/images/blob/master/webapp.png)
