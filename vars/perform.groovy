def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    node {
        config = executePipeline {
            pipeline = [
                    'init' : [
                                actions: 'Config.perform',
                                params: [param1: 'test']
                             ],
//                    'build': ['Docman.deploy', 'Docman.info'],
//                    'ops'  : ['Druflow.deployFlow']
            ]
            p = params
        }
        dump(config)
    }
}

