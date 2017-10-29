package com.github.aroq.drupipe

class DrupipeStage implements Serializable {

    String name

    ArrayList<DrupipeActionWrapper> actions = []

    DrupipePipeline pipeline

    def execute(body = null) {
        def script = script
        script.stage(name) {
            script.gitlabCommitStatus(name) {
                if (body) {
                    // TODO: recheck it.
                    body()
                }
                pipeline.block.stage = this
                if (actions) {
                    try {
                        for (a in this.actions) {
                            a.pipeline = pipeline
                            (new DrupipeActionWrapper(a)).execute()
                        }
                    }
                    catch (e) {
                        script.echo e.toString()
                        throw e
                    }
                }
            }
        }
    }

}
