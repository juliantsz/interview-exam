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

def buildImage(String credentials, String server, String artifactId, String version) {
    writeFile file: 'Dockerfile', text:libraryResource("Dockerfile")
    writeFile file: 'buildImage.sh', text:libraryResource("buildImage.sh")
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