#!groovy

import com.github.aroq.drupipe.DrupipePipeline

def call(pipe) {
    if (!pipe.params) {
        pipe.params = [:]
    }

    (new DrupipePipeline(pipe)).execute()
}
