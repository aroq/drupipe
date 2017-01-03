#!groovyf

def call(pipeline) {
    drupipe() { config ->
        node(config.nodeName) {
            withDrupipeDocker(config) {
                drupipeStages(pipeline.stages)
            }
        }
    }
}
