def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    stage 'build'

    for (action in params.actions) {
        try {
            def values = action.split("\\.")
            def actionInstance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.${values[0]}", true, false )?.newInstance()
            def methodName = values[1]
//            echo methodName
//
//            actionInstance.metaClass.methods.each { method ->
//                echo method.name
//                if (method.name == methodName) {
//                    method.invoke(actionInstance, 'bar')
//                }
//            }
            actionInstance."$methodName"()
            echo "OK: ${actionInstance}"
        }
        catch (err) {
            echo "Action ${action} is not exists."
            echo err.toString()
            throw err
        }
    }

}
