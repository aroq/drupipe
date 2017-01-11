def call(name, config, body) {
//    utils = new com.github.aroq.drupipe.Utils()
    new Stage(name: name, params: config).execute(body)
//    utils._executeStage(name, config, body)
}

