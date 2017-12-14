package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class DrushFeaturesList extends BaseAction {

    String state = 'Overridden'

    def runCommand() {
        def features = []

        // TODO: Don't use context root for action specific params.
        action.pipeline.context.env.drush_command = 'fl --format=json'
        def fl_result = this.script.drupipeAction([action: "Drush.runCommand", params: [store_result: true]], action.pipeline)

        def jsonOutput = this.script.readJSON(text: fl_result.stdout)
        jsonOutput.each { key, feature->
            if (feature.containsKey('state') && feature['state'] == this.state) {
                features << feature
            }
        }

        if (features.size() > 0) {
            def exception_table = "|Name|Machine name|\n|:---|:---|\n"
            features.each { feature ->
                exception_table = exception_table + "|${feature['name']}|${feature['feature']}|\n"

                this.action.pipeline.context.env.drush_command = 'fd ' + feature['feature']
                this.script.drupipeAction([action: "Drush.runCommand"], this.action.pipeline)
            }

            throw new Exception("OVERRIDEN FEATURES:\n\n${exception_table}")
        }
        else {
            def message = "DRUSH FEATURES LIST: No overridden features."
            this.script.echo(message)
            return [result: message]
        }
    }
}
