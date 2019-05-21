#!groovy

import com.github.aroq.drupipe.DrupipeController

def call(blocks = [:], body) {
    // "params" is the globally available Jenkins params map (immutable).
    (new DrupipeController(blocks: blocks, script: this, params: params, env: env)).execute(body)
}
