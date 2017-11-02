#!groovy

import com.github.aroq.drupipe.DrupipePipeline

def call(blocks = [:], body) {
    // "params" is the globally available Jenkins params map (immutable).
    (new DrupipePipeline(blocks: blocks, script: this, params: params)).execute(body)
}
