#!groovy

import com.github.aroq.drupipe.DrupipePipeline

@Grab('org.yaml:snakeyaml:1.9')
import org.yaml.snakeyaml.Yaml

def call(yamlFileName) {
    def pipe
    node('master') {
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
