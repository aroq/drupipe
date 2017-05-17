#!groovy

import com.github.aroq.drupipe.DrupipePipeline

def call() {
    (new DrupipePipeline([script: this, params: params])).execute()
}
