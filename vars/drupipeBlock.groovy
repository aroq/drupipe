#!groovy

import com.github.aroq.drupipe.DrupipeBlock

def call(blockParams = [:], config, body) {
    (new DrupipeBlock(blockParams)).execute(config, body)
}
