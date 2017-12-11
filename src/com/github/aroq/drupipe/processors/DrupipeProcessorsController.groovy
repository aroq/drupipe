package com.github.aroq.drupipe.processors

class DrupipeProcessorsController implements Serializable {

    ArrayList<DrupipeProcessor> processors

    DrupipeParamProcessor drupipeParamProcessor

    def utils

    def process(context, object, parent, key = 'params', mode = 'config') {
//        controller.drupipeLogger.log "DrupipeProcessorsController->processItem"
        if (object instanceof Map) {
            for (DrupipeProcessor processor in processors) {
                object = processor.process(context, object, parent, key, mode)
            }

            def objects = object.collect { it }

            for (int i = 0; i < objects.size(); i++) {
                def item = objects[i]
                object[item.key] = this.process(context, item.value, item.key, key, mode)
            }
        }
        else if (object instanceof List) {
            object = object.collect { this.process(context, it, parent, key, mode) }
        }
        object
    }

}
