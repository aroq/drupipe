#!groovy

import com.github.aroq.drupipe.DrupipePipeline

def call(context = [:], body) {
    // "params" is the globally available Jenkins params map (immutable).
    (new DrupipePipeline(context: context, script: this, params: params)).execute(body)
}
