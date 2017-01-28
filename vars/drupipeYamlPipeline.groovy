#!groovy

import com.github.aroq.drupipe.DrupipePipeline

@Grab('org.yaml:snakeyaml:1.9')
import org.yaml.snakeyaml.Yaml

def call(yamlFileName = null) {
    def pipe
    node('master') {
        checkout scm
        yamlFileName = yamlFileName ? yamlFileName : "${env.JOB_BASE_NAME}.yaml"
        pipe = drupipeGetPipeline(readFile("docroot/config/pipelines/${yamlFileName}"))
    }
    pipe.execute()
}

@NonCPS
def drupipeGetPipeline(yamlFile) {
    Yaml yaml = new Yaml();
    echo "yamlFile: ${yamlFile}"
    DrupipePipeline drupipePipeline = yaml.loadAs(yamlFile, DrupipePipeline.class);
    drupipePipeline.script = this
    drupipePipeline.params = params
    return drupipePipeline
}
