#!groovy

import com.github.aroq.drupipe.DrupipeBlock

def call(blockParams = [:], config, body) {
    echo "blockParams: ${blockParams}"
    echo "config: ${config}"
    echo "body: ${body}"
    (new DrupipeBlock(blockParams)).execute(config, body)
}
