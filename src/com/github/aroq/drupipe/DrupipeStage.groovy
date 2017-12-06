package com.github.aroq.drupipe

class DrupipeStage implements Serializable {

    String name

    ArrayList<DrupipeActionWrapper> actions = []

    DrupipeController pipeline

    def execute(body = null) {
        def script = pipeline.script
        script.echo "DrupipeStage execute - ${name}"
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
                            script.echo "DrupipeStage -> DrupipeAction execute - ${a.name}"
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
