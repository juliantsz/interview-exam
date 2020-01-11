def call() {
    pipeline {
        stages {
            stage('Init'){
                steps {
                    script {
                        println("Hello world")
                    }
                }
            }
        }
    }
}