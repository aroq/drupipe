#!groovy

import com.github.aroq.drupipe.DrupipeBlock

def call(blockParams = [:], config, body) {
    echo "drupipeBlock: ${this}"
    echo "drupipeBlock.test: ${this.test}"
    (new DrupipeBlock(blockParams)).execute(config, body)
}
