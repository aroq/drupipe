def call(stages, config) {
    utils = new com.github.aroq.drupipe.Utils()

    try {
        utils._pipelineNotify(config)
        config << utils.executeStages(stages, config)
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        utils._pipelineNotify(config, currentBuild.result)
        config
    }
}

