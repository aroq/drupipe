package com.github.aroq.workflowlibs.actions

def perform(params) {
    def source = params.source
    def result
    switch (source.type) {
        case 'git':
            result = dir(params.source.path) {
                git url: params.source.url, branch: params.source.branch
            }
            break

        case 'dir':
            result = source.dir
            break

        case 'docmanDocroot':
            result = executeAction(utils.processPipelineAction([action: 'Docman.init', params: [dir: 'docroot']])) {
                p = params
            }
            break
    }
    if (!params.sources) {
        params.sources = [:]
    }
    if (result) {
        params.sources << source
    }
    params
}

