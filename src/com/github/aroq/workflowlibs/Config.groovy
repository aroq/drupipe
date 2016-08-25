package com.github.aroq.workflowlibs

def perform(params) {
    config = configHelper {
        p = params
    }
    config.workspace = pwd()

    if (config.configProvider == 'docman') {
        def docman = new com.github.aroq.workflowlibs.Docman()
        config.initDocman = false
        docman.info(config)
    }

    return config
}

