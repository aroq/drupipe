import com.github.aroq.workflowlibs.Action

def call(Action action, body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    if (params.p) {
        params << params.p
        params.remove('p')
    }

    try {
        if (!action.params) {
            action.params = [:]
        }
        utils = new com.github.aroq.workflowlibs.Utils()
        dump(params << action.params, "${action.name} action params")
        // TODO: configure it:
        def actionFile = null
        if (params.sourcesList) {
            echo "Sources class: ${params.sourcesList.getClass()}"
//            sources = utils.processSources(params.sources)
//            echo "Sources class: ${sources.getClass()}"
//            dump(sources, 'SOURCES')
            for (i = 0; i < sources.size(); i++) {
                source = sources[i]
//                dump(source, 'SOURCE')
                fileName = sourcePath(params, source.name, 'config/pipelines/actions/' + action.name + '.groovy')
                echo "Action file name to check: ${fileName}"
                if (fileExists(fileName)) {
                    actionFile = load(fileName)
                    actionResult = actionFile."$action.methodName"(params << action.params)
                }
            }
        }
        if (!actionFile) {
            try {
                def actionInstance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.actions.${action.name}", true, false )?.newInstance()
                actionResult = actionInstance."$action.methodName"(params << action.params)
            }
            catch (err) {
                echo err.toString()
                throw err
            }
        }

        if (actionResult) {
            if (isCollectionOrList(actionResult)) {
                params << actionResult
            }
            else {
                params << ["${action.name}.${action.methodName}": actionResult]
            }
        }
        dump(params, "${action.name} action result")
        params
    }
    catch (err) {
        echo err.toString()
        throw err
    }
}

boolean isCollectionOrList(object) {
    object instanceof java.util.Collection || object instanceof java.util.List || object instanceof java.util.LinkedHashMap || object instanceof java.util.HashMap
}
