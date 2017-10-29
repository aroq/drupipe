#!groovy

import com.github.aroq.drupipe.DrupipeBlock

def call(blockParams = [:], body) {
    (new DrupipeBlock(blockParams)).execute(body)
}
