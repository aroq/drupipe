#!groovy

package com.github.aroq.drupipe.DrupalPipeline

@Grab('org.yaml:snakeyaml:1.9')
import org.yaml.snakeyaml.Yaml

def call(yamlFileName) {
    def pipe = drupipeGetPipeline(readFile("docroot/config/pipelines/${yamlFile}"))
    drupipePipeline(pipe)
}

@NonCPS
def drupipeGetPipeline(yamlFile) {
    Yaml yaml = new Yaml();
    DrupalPipeline drupipePipeline = yaml.loadAs(yamlFile, DrupalPipeline.class);
    return drupipePipeline
}
