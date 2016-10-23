def call(params = [:], body) {

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    node(params.nodeName) {
//        withCredentials([[$class: 'FileBinding', credentialsId: 'id_rsa', variable: 'ID_RSA_FILE']]) {
            def drudock = docker.image('aroq/drudock:1.0.1')
            drudock.pull()
            drudock.inside('--user root:root') {
                echo "Credentials: ${params.credentialsID}"
                sshagent([params.credentialsID]) {
                    echo 'ssh'
                    sh "ssh -v git@code.adyax.com"

                    echo 'git without credentials'
                    git url: 'git@code.adyax.com:CI-Sample-Multirepo/config.git', branch: 'master'

                    echo 'git with credentials'
                    git credentialsId: params.credentialsID, url: 'git@code.adyax.com:CI-Sample-Multirepo/config.git', branch: 'master'

                    echo 'checkout scm'
                    checkout scm
                    if (params.pipeline) {
                        params = executePipeline {
                            noNode = true
                            credentialsID = params.credentialsID
                            pipeline = params.pipeline
                        }
                    }
                    body()
                }
            }
        }
//    }

    params
}
