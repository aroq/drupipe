#!groovy

import com.github.aroq.drupipe.DrupipePipeline

def call(LinkedHashMap commandParams = [:]) {
    echo "Pipeline type: ${commandParams.type}"
    def pipe
    def projectConfig = 'docroot/config'
    node('master') {
        checkout scm
        def yamlFileName
        if (commandParams.type == 'selenese') {
            yamlFileName = "pipelines/${commandParams.type}.yaml"
        }
        else {
            yamlFileName = params.yamlFileName ? params.yamlFileName : "pipelines/${env.JOB_BASE_NAME}.yaml"
        }

        def p = readYaml(file: "${projectConfig}/${yamlFileName}")
        pipe = drupipeGetPipelineObject(p)

        utils = new com.github.aroq.drupipe.Utils()
    }
    pipe.execute()
}

def drupipeGetPipelineObject(p) {
    drupipePipeline = new DrupipePipeline(p)
    drupipePipeline.script = this
    drupipePipeline.params = params
    drupipePipeline
}

