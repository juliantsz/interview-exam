def call() {
    pipeline {
        stages {
            stage('Init'){
                println("Hello world")
            }
        }
    }
}