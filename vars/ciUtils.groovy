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
    writeFile file: 'Dockerfile', text:libraryResource("docker/Dockerfile")
    writeFile file: 'buildImage.sh', text:libraryResource("bash/buildImage.sh")
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

def deployPod(String credentials, String server) {
    writeFile file: 'pod.yml', text:libraryResource("pod/pod.yml")
    writeFile file: 'service.yml', text:libraryResource("pod/service.yml")
    def remote = [:]
    remote.name = "${server}"
    remote.host = "${server}"
    remote.allowAnyHosts = true
    withCredentials([usernamePassword(credentialsId: "${credentials}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        remote.user = USERNAME
        remote.password = PASSWORD
        sshPut remote: remote, from: "${WORKSPACE}/pod.yml", into: '/home/cloud_user/'
        sshPut remote: remote, from: "${WORKSPACE}/service.yml", into: '/home/cloud_user/'
        sshCommand remote: remote, command: "cd /home/cloud_user/; kubectl apply -f pod.yml"
        sshCommand remote: remote, command: "cd /home/cloud_user/; kubectl apply -f service.yml"
        sshCommand remote: remote, command: "cd /home/cloud_user/; rm pod.yml service.yml"
    }
}