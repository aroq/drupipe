def call(params = [:], body) {

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    echo "Credentials: ${params.credentialsID}"

    node(params.nodeName) {
        withCredentials([[$class: 'FileBinding', credentialsId: 'id_rsa', variable: 'ID_RSA_FILE']]) {
            def drudock = docker.image('aroq/drudock:1.0.1')
            drudock.pull()
            drudock.inside('--user root:root') {
                sshagent([params.credentialsID]) {
                    if (params.pipeline) {
                        params = executePipeline {
                            noNode = true
                            echo "Credentials: ${params.credentialsID}"
                            credentialsID = params.credentialsID
                            pipeline = params.pipeline
                        }
                    }
                    body()
                }
            }
        }
    }

    params
}
