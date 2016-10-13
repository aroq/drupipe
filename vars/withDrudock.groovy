def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    node(params.nodeName) {
        withCredentials([[$class: 'FileBinding', credentialsId: 'id_rsa', variable: 'ID_RSA_FILE']]) {
            def drudock = docker.image('aroq/drudock:1.0.1')
            drudock.pull()
            drudock.inside('--user root:root') {
                sshagent([params.gitCredentialsID]) {
                    if (params.pipeline) {
                        params = executePipeline {
                            noNode = true
                            gitCredentialsID = params.gitCredentialsID
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
