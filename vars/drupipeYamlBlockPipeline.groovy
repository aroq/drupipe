#!groovy

import com.github.aroq.drupipe.DrupipePipeline

def call() {
    DrupipePipeline([script: this, params: params]).execute()
}

