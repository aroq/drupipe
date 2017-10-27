#!groovy

import com.github.aroq.drupipe.DrupipeStage

def call(name, context, body) {
    new DrupipeStage(name: name, context: context).execute(body)
}

