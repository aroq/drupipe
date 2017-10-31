#!groovy

import com.github.aroq.drupipe.DrupipeBlock
import com.github.aroq.drupipe.DrupipePipeline

def call(blockParams = [:], DrupipePipeline pipeline, body) {
    blockParams.pipeline = pipeline
    (new DrupipeBlock(blockParams)).execute(body)
}
