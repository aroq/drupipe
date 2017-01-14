#!groovy

import com.github.aroq.drupipe.DrupipeBlock

def call(blockParams = [:], config, body) {
    echo "drupipeBlock: ${this}"
    (new DrupipeBlock(blockParams)).execute(config, body)
}
