package com.github.aroq.drupipe

class DrupipeStage implements Serializable {

    String name

    ArrayList<DrupipeAction> actions = []

    HashMap params = [:]

    def execute(params, body = null) {
        def utils = new com.github.aroq.drupipe.Utils()
        this.params = params
        this.params.pipeline.script.stage(name) {
            this.params.pipeline.script.gitlabCommitStatus(name) {
                if (body) {
                    this.params << body()
                }
                this.params << ['stage': this]
                if (actions) {
                    try {
                        for (a in this.actions) {
                            def action = new DrupipeAction(a)

                            try {
                                this.params.pipeline.script.echo "BEFORE ACTION: ${name}"
                                this.params.pipeline.script.sh("""
cd docroot/config; git diff; git show""")
                            }
                            catch (e) {

                            }
                            this.params << action.execute(this.params)
                            try {
                                this.params.pipeline.script.echo "AFTER ACTION: ${name}"
                                this.params.pipeline.script.sh("""
cd docroot/config; git diff; git show""")
                            }
                            catch (e) {

                            }
                        }
                        this.params
                    }
                    catch (e) {
                        this.params.pipeline.script.echo e.toString()
                        throw e
                    }
                }
            }
        }
        this.params
    }

}

