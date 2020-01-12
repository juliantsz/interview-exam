# Reto Devops

### Setup de Jenkins

Para tener un servidor de Jenkins en el cual trabajar opte por la opción de contenedor con Docker corriendo en mi local, con la portabilidad de Docker este contenedor de Jenkins puede ser levantado en la nube o on-premise. Ejecutando este archivo [yaml](https://github.com/juliantsz/jenkins-shared-library/blob/master/src/jenkins-compose.yml) con `docker-compose`. En el explorador colocamos la dirección `ip` del servidor (127.0.0.1 para localhost) seguido del puerto `8080`, `127.0.0.1:8080`. De esta manera tenemos Jenkins.

##### plugins utilizados
[pipeline-utility-steps](https://plugins.jenkins.io/pipeline-utility-steps) Este plugin nos permite leer y escribir archivos como `yaml`, `json`, `properties`, `pom.xml`, etc. En este proyecto fue utilizado para leer el archivo `pom.xml` dentro del repositorio para poder obtener el `artifactId` y `version` del proyecto. De esta manera las imágenes Docker son generadas dinámicamente leyendo este archivo.

[SSH Pipeline Steps](https://plugins.jenkins.io/ssh-steps) Este plugin nos permite copiar, obtener, eliminar archivos y ejecutar comandos de servidores remotos utilizando credenciales definidas en las credenciales de Jenkins. Todo esto de manera segura y sin quemas credenciales en el codigo. Las credenciales al estar en un lugar centralizado, se pueden actualizar facilmente sin tener que modificar el código.

[Config File Provider Plugin](https://wiki.jenkins.io/display/JENKINS/Config+File+Provider+Plugin) Nos permite inyectar archivos de configuración en tiempo de ejecución del pipeline. Por ejemplo `settings.xml`. De esta manera se puede modificar sin tener que ingresar a un servidor o contenedor.

##### jenkins-shared-libraries
Jenkins puede ser ejecutada de muchas maneras. Una de estas es con un `Jenkinsfile` sin embargo el problema es que este archivo es guardado en el repositorio donde se encuentra el código de los desarrolladores, corriendo el riesgo de ser modificado o eliminado por alguien distinto al DevOps del proyecto. Con [jenkins-shared-libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) se define un repositorio donde se versiona el código utilizado por el DevOps. De esta manera se tiene por un lado el código de los desarrolladores y el código del o los DevOps separados evitando conflictos. Para su uso es necesario cierta configuración y estructura del repositorio.

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
@Library llamamos la libreria configurada previamante, tener en cuenta que esta usa la rama definida. En este caso `master`. Luego se llama al archivo groovy en este caso `ciMaven()`. De esta manera se tiene un mayor control sobre el código utilizado por el DevOps y no mezclarlo con el de los desarrolladores.


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
`ciUtils.gitCheckout()` es una función definida del archivo `vars/ciUtils`. Esto ayuda a la reutilización de código simplemente llamando la función y pasando los parámetros necesarios.

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
[sonar-home](https://github.com/juliantsz/images/blob/master/sonar.png)
[sonar-detailed](https://github.com/juliantsz/images/blob/master/sonar-overview.png)


