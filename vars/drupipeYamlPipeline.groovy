#!groovy

import com.github.aroq.drupipe.DrupipePipeline

@Grab('org.yaml:snakeyaml:1.9')
import org.yaml.snakeyaml.Yaml

def call(yamlFileName = null) {
    def pipe
    node('master') {
        checkout scm
        yamlFileName = yamlFileName ? yamlFileName : env.JOB_BASE_NAME
        pipe = drupipeGetPipeline(readFile("docroot/config/pipelines/${yamlFileName}"))
    }
    drupipePipeline(pipe)
}

@NonCPS
def drupipeGetPipeline(yamlFile) {
    Yaml yaml = new Yaml();
    DrupipePipeline drupipePipeline = yaml.loadAs(yamlFile, DrupipePipeline.class);
    return drupipePipeline
}
