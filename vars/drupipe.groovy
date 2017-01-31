#!groovy

import com.github.aroq.drupipe.DrupipePipeline

def call(context = [:], body) {
    // "params" is the globally available Jenkins params map (immutable).
    script.echo "DRUPIPE: BEFORE: debugEnabled: ${params.debugEnabled}"
    params.debugEnabled = params.debugEnabled && params.debugEnabled != '0' ? true : false
    script.echo "DRUPIPE: debugEnabled: ${params.debugEnabled}"
    context.pipeline = new DrupipePipeline(context: context, script: this, params: params)
    context.pipeline.execute(body)
}
