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
                        POM = readMavenPom file: 'pom.xml'
                    }
                }
            }
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
                        sh "ls -l"
                        sh "ls -l target/"
                    }
                }
            }
            stage('Build Docker Image') {
                steps {
                    script {
                        println("EC2 IP ADDRESS ${env.ec2-ip}")
                        ciUtils.buildImage(
                            "ec2-user",//credentials
                            "${env.ec2-ip}"//server
                        )
                    }
                }
            }
            /*stage('Maven Scan') {
                steps {
                    script {
                        sh "mvn sonar:sonar"
                    }
                }
            }*/
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