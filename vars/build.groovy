def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    stage 'build'

    for (action in params.actions) {
        try {
//            actionObject = new(com.github.aroq.workflowlibs."${action}"())
            def instance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.${action}", true, false )?.newInstance()
            echo "OK: ${instance}"
        }
        catch (err) {
            echo "Action ${action} is not exists."
            echo err.toString()
        }
    }

}
