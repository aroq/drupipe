#!groovy

import com.github.aroq.drupipe.DrupipeBlock

def call(blockParams = [:], context, body) {
    (new DrupipeBlock(blockParams)).execute(context, body)
}
