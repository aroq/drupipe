def call(params = [:], body) {

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    echo "Credentials: ${params.credentialsID}"

    node(params.nodeName) {
        echo "Credentials2: ${params.credentialsID}"
        withCredentials([[$class: 'FileBinding', credentialsId: 'id_rsa', variable: 'ID_RSA_FILE']]) {
            echo "Credentials3: ${params.credentialsID}"
            def drudock = docker.image('aroq/drudock:1.0.1')
            drudock.pull()
            drudock.inside('--user root:root') {
                echo "Credentials4: ${params.credentialsID}"
                sshagent([params.credentialsID]) {
                    echo "Credentials5: ${params.credentialsID}"
                    if (params.pipeline) {
                        echo "Credentials6: ${params.credentialsID}"
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
    }

    params
}
