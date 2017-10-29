#!groovy

import com.github.aroq.drupipe.DrupipeStage

def call(name, pipeline, body) {
    new DrupipeStage(name: name, pipeline: pipeline).execute(body)
}

