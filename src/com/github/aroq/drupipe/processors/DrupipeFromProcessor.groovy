package com.github.aroq.drupipe.processors

import com.github.aroq.drupipe.DrupipeController

class DrupipeFromProcessor {

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
            result = controller.utils.merge(result, getFromPathItem(object, pathItem, key))
            parentItem = pathItem
        }
        result
    }

    def processFromItem(context, result, from, parent) {
        // TODO: check about .params.
        def fromObject = getFrom(context.params, from)
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
            fromObject = processFrom(context, fromObject, parent)
            result = utils.merge(fromObject, result)
        }
        result
    }

    def processFrom(context, obj, parent) {
        def result = obj
        if (obj.containsKey('from')) {
            if (obj.from instanceof CharSequence) {
                result = processFromItem(context, result, obj.from, parent)
            }
            else {
                for (item in obj.from) {
                    def fromObject = utils.deepGet(context, 'params.' + item)
                    if (fromObject) {
                        result = processFromItem(context, result, item, parent)
                    }
                }
            }
            result.remove('from')
        }
        result
    }

    def processConfigItem(context, object, parent) {
        if (object instanceof Map) {
            object = processFrom(context, object, parent)
            for (item in object) {
                object[item.key] = processConfigItem(context, item.value, item.key)
            }
        }
        else if (object instanceof List) {
            object = object.collect { processConfigItem(context, it, parent) }
        }
        object
    }
}
