#!groovy

import com.github.aroq.drupipe.DrupipePipeline

@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor

def call(LinkedHashMap commandParams = [:]) {
    echo "Pipeline type: ${commandParams.type}"
    def pipe
    def projectConfig = 'docroot/config'
    node('master') {
        checkout scm
        yamlFileName = commandParams.yamlFileName ? commandParams.yamlFileName : "${env.JOB_BASE_NAME}.yaml"
        pipe = drupipeGetPipeline(readFile("${projectConfig}/pipelines/${yamlFileName}"))
    }
    pipe.execute()
}

@NonCPS
def drupipeGetPipeline(yamlFile) {
    Yaml yaml = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()));
    echo "yamlFile: ${yamlFile}"
    drupipePipeline = yaml.loadAs(yamlFile, DrupipePipeline.class);
    drupipePipeline.script = this
    drupipePipeline.params = params
    return drupipePipeline
}
