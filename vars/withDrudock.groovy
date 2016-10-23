def call(params = [:], body) {

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    node(params.nodeName) {
        def drudock = docker.image('aroq/drudock:1.0.1')
        drudock.pull()
        drudock.inside('--user root:root') {
            sshagent([params.credentialsID]) {
                if (params.pipeline) {
                    params = executePipeline {
                        noNode = true
                        credentialsID = params.credentialsID
                        pipeline = params.pipeline
                    }
                }
                params << body()
            }
        }
    }

    params
}
