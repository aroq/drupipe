#!groovy

import com.github.aroq.drupipe.DrupipeBlock

def call(blockParams = [:], pipeline, body) {
    (new DrupipeBlock(blockParams, pipeline)).execute(body)
}
