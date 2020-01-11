def call() {
    
    pipeline {
        agent {
            node {
                label "master"
                customWorkspace "/var/jenkins_home/workspace/${env.BUILD_TAG}"
            }
        }
        options {
            timestamps() 
        }
        stages {
            stage('Init'){
                steps {
                    script {
                        println("Hello world")
                    }
                }
            }
        }
        post {
            cleanup{
                deleteDir()
            }
        }
    }
}