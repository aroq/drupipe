package com.github.aroq.drupipe.processors

class DrupipeParamProcessor implements Serializable {

    com.github.aroq.drupipe.Utils utils

    @NonCPS
    def interpolateCommand(String command, action, context) {
        def prepareFlags = { flags ->
            prepareFlags(flags)
        }

        def binding = [context: context, actions: context.actions ? context.actions : [:], action: action, prepareFlags: prepareFlags]
        def engine = new groovy.text.SimpleTemplateEngine()
        def template = engine.createTemplate(command).make(binding)
        template.toString()
    }

    @NonCPS
    def processActionParams(action, context, ArrayList prefixes, ArrayList path = []) {
        def params
        if (path) {
            params = path.inject(action.params, { obj, prop ->
                if (obj && obj[prop]) {
                    obj[prop]
                }
            })
        }
        else {
            params = action.params
        }

        for (param in params) {
            if (param.value instanceof CharSequence) {
                param.value = getActionParam(params[param.key], context, prefixes.collect {
                    [it, param.key.toUpperCase()].join('_')
                })
                param.value = interpolateCommand(param.value, action, context)
            } else if (param.value instanceof Map) {
                processActionParams(action, context, prefixes.collect {
                    [it, param.key.toUpperCase()].join('_')
                }, path + param.key)
            } else if (param.value instanceof List) {
                for (def i = 0; i < param.value.size(); i++) {
                    param.value[i] = interpolateCommand(param.value[i], action, context)
                }
            }
        }
    }

    @NonCPS
    def getActionParam(param, context, prefixes) {
        def result = param
        prefixes.each {
            if (context.env && context.env?.containsKey(it)) {
                result = context.env[it]
            }
        }

        result
    }

    @NonCPS
    def prepareFlags(flags) {
        flags.collect { k, v ->
            v.collect { subItem ->
                "${k} ${subItem}".trim()
            }.join(' ').trim()
        }.join(' ')
    }

}
