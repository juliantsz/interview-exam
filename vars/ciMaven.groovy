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
                        withMaven(
                            mavenSettingsConfig: 'maven-settings') {
                            sh "mvn clean verify sonar:sonar"
                        }
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