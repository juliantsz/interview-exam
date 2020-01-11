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

def buildImage(String credentials, String server) {
    def remote = [:]
    remote.name = "localhost"
    remote.host = "${server}"
    remote.allowAnyHosts = true
    withCredentials([usernamePassword(credentialsId: "${credentials}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        remote.user = 'USERNAME'
        remote.password = 'PASSWORD'
        sshCommand remote: remote, command: "cd /home/julian/Documents/Developer ; ls -l"
    }
}