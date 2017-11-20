package com.github.aroq.drupipe.processors

class DrupipeProcessorsController implements Serializable {

    ArrayList<DrupipeProcessor> processors

    def utils

    def process(context, object, parent, key = 'params', mode = 'config') {
        if (object instanceof Map) {
            for (DrupipeProcessor processor in processors) {
                utils.log "DrupipeProcessorsController->process BEFORE1 serializeAndDeserialize"
                utils.serializeAndDeserialize(context)
                utils.log "DrupipeProcessorsController->process AFTER1 serializeAndDeserialize"
                object = processor.process(context, object, parent, key, mode)
                utils.log "DrupipeProcessorsController->process BEFORE2 serializeAndDeserialize"
                utils.serializeAndDeserialize(context)
                utils.log "DrupipeProcessorsController->process AFTER2 serializeAndDeserialize"
            }
            utils.log "DrupipeProcessorsController->process BEFORE2.1 serializeAndDeserialize"
            utils.serializeAndDeserialize(context)
            utils.log "DrupipeProcessorsController->process AFTER2.1 serializeAndDeserialize"
            for (item in object) {
                utils.log "DrupipeProcessorsController->process BEFORE3 serializeAndDeserialize"
                utils.serializeAndDeserialize(context)
                utils.log "DrupipeProcessorsController->process AFTER3 serializeAndDeserialize"
                object[item.key] = this.process(context, item.value, item.key, key, mode)
                utils.log "DrupipeProcessorsController->process BEFORE4 serializeAndDeserialize"
                utils.serializeAndDeserialize(context)
                utils.log "DrupipeProcessorsController->process AFTER4 serializeAndDeserialize"
            }
        }
        else if (object instanceof List) {
            utils.log "DrupipeProcessorsController->process BEFORE5 serializeAndDeserialize"
            utils.serializeAndDeserialize(context)
            utils.log "DrupipeProcessorsController->process AFTER5 serializeAndDeserialize"
            object = object.collect { this.process(context, it, parent, key, mode) }
            utils.log "DrupipeProcessorsController->process BEFORE6 serializeAndDeserialize"
            utils.serializeAndDeserialize(context)
            utils.log "DrupipeProcessorsController->process AFTER6 serializeAndDeserialize"
        }
        object
    }

}
