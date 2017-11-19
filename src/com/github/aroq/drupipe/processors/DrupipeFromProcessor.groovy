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

    def getFrom(object, path, key = 'params') {
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

    def processFromItem(context, result, from, parent, key = 'params') {
        // TODO: check about .params.
//        from = 'params.' + from

//        utils.log "Process from: ${from}"

        def processorParams = getFrom(context, from, 'processors')
        if (processorParams) {
//            utils.debugLog(context, processorParams, 'processFromItem->processorParams', [debugMode: 'json'], [], true)
            def keyMode = utils.deepGet(processorParams, "${this.include_key}.mode")

            if (keyMode == this.mode) {
//                utils.log "DrupipeFromProcessor->processFromItem() ${from} processed as mode is ${keyMode}, include_key: ${this.include_key}"
                def fromObject = getFrom(context, from, key)
                if (fromObject) {
                    if (parent == 'job') {
                        fromObject.name = from
                    }
                    if (parent == 'pipeline') {
                        fromObject.name = from
                    }
                    if (parent == 'containers') {
                        fromObject.name = from
                    }
                    if (parent == 'blocks') {
                        fromObject.name = from
                    }
                    // Set name to 'from' if parent is 'actions'.
                    if (parent in ['actions', 'pre_actions', 'post_actions']) {
                        def action = from - 'params.actions.'
                        def values = action.split("\\.")
                        if (values.size() > 1) {
                            fromObject.name = values[0]
                            fromObject.methodName = values[1]
                            fromObject.configVersion = 2
                        }
                    }
                    fromObject = process(context, fromObject, parent, key)
                    result = utils.merge(fromObject, result)
                }
                result.remove(this.include_key)
            }
            else {
//                utils.log "DrupipeFromProcessor->processFromItem() ${from} skipped as mode is ${keyMode}, include_key: ${this.include_key}"
            }
        }
        else {
            utils.log "DrupipeFromProcessor->processFromItem() no processorParams defined"
        }

        result
    }

    def process(context, obj, parent, key = 'params', mode = 'config') {
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
