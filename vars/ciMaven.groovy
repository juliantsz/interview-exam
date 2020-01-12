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