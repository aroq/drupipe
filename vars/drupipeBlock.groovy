#!groovy

import com.github.aroq.drupipe.DrupipeBlock
import com.github.aroq.drupipe.DrupipeController

def call(blockParams = [:], DrupipeController pipeline, body) {
    blockParams.pipeline = pipeline
    (new DrupipeBlock(blockParams)).execute(body)
}
