package com.github.aroq.drupipe.processors

class DrupipeFromProcessor implements Serializable, DrupipeProcessor {

    com.github.aroq.drupipe.Utils utils

    String mode

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
        def processParams = getFrom(context.params, from, 'process_params')
        if (processParams && processParams.from) {
            def stop = true
            if () {

            }
        }
        def fromObject = getFrom(context.params, from, key)
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
                def action = from - 'actions.'
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
        result
    }

    def process(context, obj, parent, key = 'params', mode = 'config') {
        this.mode = mode
        def result = obj
        if (obj.containsKey('from')) {
            if (obj.from instanceof CharSequence) {
                result = processFromItem(context, result, obj.from, parent, key)
            }
            else {
                for (item in obj.from) {
                    def fromObject = utils.deepGet(context, 'params.' + item)
                    if (fromObject) {
                        result = processFromItem(context, result, item, parent, key)
                    }
                }
            }
            result.remove('from')
        }
        result
    }

}
