import com.github.aroq.drupipe.DrupipePipeline

def call(context = [:], body) {
    context.pipeline = new DrupipePipeline(context: context, script: this)
    timestamps {
        if (!context['Config_perform']) {
            node('master') {
                configParams = drupipeAction([action: 'Config.perform'], context.clone() << params)
                context << (configParams << context)
            }
        }

        if (params.force == '11') {
            echo 'FORCE REMOVE DIR'
            deleteDir()
        }
        if (params.checkoutSCM) {
            checkout scm
        }

        result = body(context)

        if (result) {
            context << result
        }
    }

    configParams
}
