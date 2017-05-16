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
        def yamlFileName
        if (commandParams.type == 'selenese') {
            yamlFileName = "pipelines/${commandParams.type}.yaml"
        }
        else {
            yamlFileName = params.yamlFileName ? params.yamlFileName : "pipelines/${env.JOB_BASE_NAME}.yaml"
        }

        //pipe = drupipeGetPipeline(readFile("${projectConfig}/${yamlFileName}"))

        def p = readYaml(file: "${projectConfig}/${yamlFileName}")
        pipe = drupipeGetPipelineObject(p)
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

@NonCPS
def drupipeGetPipelineObject(p) {
//    Yaml yaml = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()));
//    def yamlDoc = Yaml.dump(p)
//    drupipePipeline = yaml.loadAs(yamlDoc, DrupipePipeline.class);
//    drupipePipeline.script = this
//    drupipePipeline.params = params
//    return drupipePipeline
    new DrupipePipeline(p)
}

