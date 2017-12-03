package com.github.aroq.drupipe.processors

class DrupipeFromProcessor implements Serializable, DrupipeProcessor {

    com.github.aroq.drupipe.Utils utils

    String mode

    String include_key

    def getFromPathItem(object, pathItem, String key) {
        def result = [:]
        if (object && object.containsKey(pathItem)) {
            if (object[pathItem] && object[pathItem].containsKey(key)) {
                result = object[pathItem][key]
            }
        }
        result
    }

    def collectKeyParamsFromJsonPath(object, path, key = 'params') {
        // Remove first '.' if exists.
        if (path[0] == '.') {
            path = path.getAt(1..path.length() - 1)
        }

        def result = [:]
        if (path instanceof CharSequence) {
            path = path.tokenize('.')
        }
        if (!path) {
            return object
        }

        def parentItem = ''
        for (pathItem in path) {
            if (parentItem) {
                object = object[parentItem]
            }
            result = utils.merge(result, getFromPathItem(object, pathItem, key))
            parentItem = pathItem
        }
        result
    }

    def getUnprocessedContext() {
        def filePath = '.unipipe/temp/context_unprocessed.yaml'
        utils.yamlFileLoad(filePath)
    }

    def processFromItem(context, result, String from, String parent, String key = 'params') {
        if (from == '.params.actions.JobDslSeed.perform') {
            utils.log "Process from: ${from}"
            utils.log "Process mode: ${mode}"
        }

        def processorParams = collectKeyParamsFromJsonPath(context, from, 'processors')
        if (processorParams) {
            utils.debugLog(context, processorParams, 'processFromItem->processorParams', [debugMode: 'json'], [], false)
            def keyMode = utils.deepGet(processorParams, "${this.include_key}.mode")

            if (keyMode == this.mode) {
                if (from == '.params.actions.JobDslSeed.perform') {
                    utils.log "DrupipeFromProcessor->processFromItem() ${from} processed as mode is ${keyMode}, include_key: ${this.include_key}"
                }

                def tempContext

                // TODO: Find cheaper way to make sure context parts were not changed during previous operations.
                if (mode == 'config') {
                    if (utils.drupipeExecutionMode() == 'jenkins') {
                        tempContext = getUnprocessedContext()
                    }
                    // For local testing.
                    else {
                        tempContext = utils.getUnprocessedContext()
                    }
                }
                else {
                    tempContext = context
                }
                if (!tempContext) {
                    throw new Exception("No tempContext is defined.")
                }

                def fromObject = collectKeyParamsFromJsonPath(tempContext, from, key)

                // TODO: Refactor it:
                if (fromObject) {
                    if (from == '.params.actions.JobDslSeed.perform') {
                        utils.log "Action parent: ${parent}"
                    }

                    if (parent == 'job') {
                        fromObject.name = from - '.params.jobs'
                    }
                    if (parent == 'pipeline') {
                        fromObject.name = from - '.params.pipelines.'
                    }
                    if (parent == 'containers') {
                        fromObject.name = from - '.params.containers.' - 'containers.'
                    }
                    if (parent == 'blocks') {
                        fromObject.name = from - '.params.blocks.'
                    }
                    // Set name to 'from' if parent is 'actions'.
                    if (parent in ['actions', 'pre_actions', 'post_actions']) {
                        def action = from - '.params.actions.'
                        def values = action.split("\\.")
                        if (values.size() > 1) {
                            fromObject.name = values[0]
                            fromObject.methodName = values[1]
                            fromObject.configVersion = 2
                        }
                    }
                    fromObject = process(context, fromObject, parent, key)
                    if (from == '.params.actions.JobDslSeed.perform') {
                        utils.debugLog(tempContext, fromObject, "fromObject", [debugMode: 'json'], [], true)
                    }
                    result = utils.merge(fromObject, result)
                    if (from == '.params.actions.JobDslSeed.perform') {
                        utils.debugLog(tempContext, fromObject, "fromObject after merge", [debugMode: 'json'], [], true)
                    }
                    result.from_processed = true
                    result.from_processed_mode = this.mode
                    result.from_source = from
                }
                else {
                    utils.log "DrupipeFromProcessor->processFromItem() FROM ${from} is not found in tempContext"
                }
                result.remove(this.include_key)
                if (from == '.params.actions.JobDslSeed.perform') {
                    utils.debugLog(tempContext, result, "result", [debugMode: 'json'], [], true)
                }
            }
            else {
                if (from == '.params.actions.JobDslSeed.perform') {
                    utils.log "DrupipeFromProcessor->processFromItem() ${from} skipped as mode is ${keyMode}, include_key: ${this.include_key}"
                }
            }
        }
        else {
            utils.log "DrupipeFromProcessor->processFromItem() no processorParams defined"
        }

        result
    }

    def process(context, obj, parent, key = 'params', mode = 'config') {
//        utils.log "DrupipeFromProcessor->processItem"
        this.mode = mode
        def result = obj
        if (obj.containsKey(this.include_key)) {
            if (obj.from instanceof CharSequence) {
                result = processFromItem(context, result, obj.from, parent, key)
            }
            else {
                for (item in obj.from) {
                    result = processFromItem(context, result, item, parent, key)
                }
            }
        }
        result
    }

}
