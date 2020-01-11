def call() {
    def POM
    pipeline {
        agent {
            node {
                label "master"
                customWorkspace "/var/jenkins_home/workspace/${env.BUILD_TAG}"
            }
        }
        tools {
            maven 'maven-3.6.3'
        }
        options {
            timestamps() 
        }
        stages {
            stage('Clone Repo') {
                steps {
                    script {
                        sh 'printenv | sort'
                        ciUtils.gitCheckout(
                            "master",//branch
                            "github",//credentials
                            "https://github.com/daticahealth/java-tomcat-maven-example.git"//url
                        )
                        sh "ls -l"
                        POM = readMavenPom file: 'pom.xml'
                    }
                }
            }
            stage('Maven Compile') {
                steps {
                    script {
                        sh """
                        mvn package
                        """
                    }
                }
            }
            stage('Build Docker Image') {
                steps {
                    script {
                        ciUtils.buildImage(
                            "root-server",//credentials
                            "${env.devopsciserver}"//server
                        )
                    }
                }
            }
        }
        post {
            cleanup{
                deleteDir()
                cleanWs()
                dir("${WORKSPACE}@tmp"){
                    deleteDir()
                }
            }
        }
    }
}