def call(name, config, body) {
    utils = new com.github.aroq.drupipe.Utils()
    utils._executeStage(name, config, body)
}

