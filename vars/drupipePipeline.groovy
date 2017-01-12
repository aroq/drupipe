#!groovy

def call(pipe) {
    if (!pipe.params) {
        pipe.params = [:]
    }

    (new DrupipePipeline(pipe)).execute()

}
