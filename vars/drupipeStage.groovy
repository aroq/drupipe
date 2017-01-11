def call(stage, config) {
    utils = new com.github.aroq.drupipe.Utils()
    utils.pipelineNotify(config)
    config << utils.executeStage(utils.processStage(stage), config)
}

