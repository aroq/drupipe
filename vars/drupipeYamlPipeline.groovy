#!groovy

import com.github.aroq.drupipe.DrupipeController

def call() {
    (new DrupipeController([script: this, params: params, env: env])).execute()
}
