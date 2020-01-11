def call() {
    
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
            stage('Clone Repo'){
                steps {
                    script {
                        ciUtils.gitCheckout(
                            "master",//branch
                            "github",//credentials
                            "https://github.com/daticahealth/java-tomcat-maven-example.git"//url
                        )
                        sh "ls -l"
                    }
                }
            }
        }
        post {
            cleanup{
                deleteDir()
                cleanWs()
            }
        }
    }
}