#!groovy

import com.github.aroq.drupipe.DrupipeController

def call() {
    (new DrupipeController([script: this, params: params])).execute()
}
