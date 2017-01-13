def call(stages, config) {
   config << config.pipeline.executeStages(stages, config)
}

