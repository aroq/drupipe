#!groovy

import com.github.aroq.drupipe.DrupipePipeline

def call() {
    podTemplate(label: 'drupipe', containers: [
        containerTemplate(name: 'drupipeContainer', image: 'golang', ttyEnabled: true, command: 'cat'),
    ]) {
        node('drupipe') {
            container('drupipeContainer') {
                echo "test"
            }
        }
    }
    (new DrupipePipeline([script: this, params: params])).execute()
}
