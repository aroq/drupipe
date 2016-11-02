def call(params = [:], body) {

    defaultParams = [imageName: 'aroq/drudock:1.0.1', args: '--user root:root']

    jsonDump(param, 'params BEFORE')
    params << defaultParams << params
    jsonDump(param, 'params AFTER')

    node(params.nodeName) {
        def image = docker.image(params.imageName)
        image.pull()
        image.inside(params.args) {
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
