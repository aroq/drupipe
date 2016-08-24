def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    stage 'build'

    for (action in params.actions) {
        try {
            def values = action.split("\\.")
            def instance = this.class.classLoader.loadClass("com.github.aroq.workflowlibs.${values[0]}", true, false )?.newInstance()
            instance.${values[1]}()
            echo "OK: ${instance}"
        }
        catch (err) {
            echo "Action ${action} is not exists."
            echo err.toString()
        }
    }

}
