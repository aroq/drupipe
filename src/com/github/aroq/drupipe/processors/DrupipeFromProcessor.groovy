package com.github.aroq.drupipe.processors

import com.github.aroq.drupipe.DrupipeController
import com.github.aroq.drupipe.DrupipeLogger

class DrupipeFromProcessor implements Serializable, DrupipeProcessor {

    com.github.aroq.drupipe.Utils utils

    DrupipeLogger drupipeLogger

    String mode

    String include_key

    DrupipeController controller

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
                if (!object) {
                    drupipeLogger.error "collectKeyParamsFromJsonPath is null for pathItem: ${pathItem} in path: ${path}, mode: ${mode}, key: ${key}"
                }
                else {
                    if (object.containsKey(parentItem)) {
                        object = object[parentItem]
                    }
                }
            }

            def from = getFromPathItem(object, pathItem, key)
            if (from) {
                result = utils.merge(result, from)
            }
            else {
                drupipeLogger.trace "getFromPathItem is null for pathItem: ${pathItem} in path: ${path}, key: ${key}"
            }
            parentItem = pathItem
        }
        result
    }

    def getUnprocessedContext() {
        def filePath = '.unipipe/temp/context_unprocessed.yaml'
        utils.yamlFileLoad(filePath)
    }

    def processFromItem(context, result, String from, String parent, String key = 'params') {
        def processorParams = collectKeyParamsFromJsonPath(context, from, 'processors')
        if (processorParams) {
            drupipeLogger.debugLog(context, processorParams, 'processFromItem->processorParams', [debugMode: 'json'])
            def keyMode = utils.deepGet(processorParams, "${this.include_key}.mode")

            if (keyMode == this.mode) {
                drupipeLogger.trace "DrupipeFromProcessor->processFromItem() ${from} processed as mode is ${keyMode}, include_key: ${this.include_key}"

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

                drupipeLogger.trace "Process from: ${from}"
                drupipeLogger.trace "Process mode: ${mode}"

                def logResult = false
                if (from == '.params.containers.common.artifact.${context.container_types.artifact.release-deploy-preprod.type}.release-deploy-preprod') {
                    drupipeLogger.debugLog(context, tempContext, 'processFromItem() - tempContext', [debugMode: 'json'], [], 'INFO')
                    logResult = true
                }

                try {
                    from = controller.drupipeProcessorsController.drupipeParamProcessor.interpolateCommand(from, [:], tempContext, logResult)
                }
                catch (Exception e) {
                    drupipeLogger.error "Error during processing ${from}"
                    drupipeLogger.debugLog(context, tempContext, 'processFromItem() - tempContext', [debugMode: 'json'], [], 'ERROR')
                    throw e
                }

                def fromObject = collectKeyParamsFromJsonPath(tempContext, from, key)

                if (logResult) {
                    drupipeLogger.debugLog(context, fromObject, 'fromObject', [debugMode: 'json'], [], 'INFO')
                }

                // TODO: Refactor it:
                if (fromObject) {
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
                        def values = action.tokenize('.')
                        if (values.size() > 1) {
                            drupipeLogger.log("Values: ${values}")
                            fromObject.methodName = values.last()
                            fromObject.name = action - ".${fromObject.methodName}"
                            fromObject.configVersion = 2
                        }
                    }
                    if (from.startsWith('.params.options')) {
                        fromObject.remove('name')
                    }
                    fromObject = process(context, fromObject, parent, key)
                    result = utils.merge(result, fromObject)
                    result.from_processed = true
                    result.from_processed_mode = this.mode
                    result.from_source = from
                }
                else {
                    drupipeLogger.log "DrupipeFromProcessor->processFromItem() FROM ${from} is not found in tempContext"
                }
                result.remove(this.include_key)
            }
            else {
//                drupipeLogger.log "DrupipeFromProcessor->processFromItem() ${from} skipped as mode is ${keyMode}, include_key: ${this.include_key}"
            }
        }
        else {
            drupipeLogger.log "DrupipeFromProcessor->processFromItem() no processorParams defined"
        }

        result
    }

    def process(context, obj, parent, key = 'params', mode = 'config') {
//        drupipeLogger.log "DrupipeFromProcessor->processItem"
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
