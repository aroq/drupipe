package com.github.aroq.drupipe.actions

import groovy.json.JsonSlurperClassic
import com.github.aroq.drupipe.DrupipeAction

class DrushFeaturesList extends BaseAction {

    def context

    def script

    def utils

    String state = 'Overridden'

    def DrupipeAction action

    def runCommand() {
        def features = []

        this.context.drush_command = 'fl --format=json'
        def fl_result = this.script.drupipeAction([action: "Drush.runCommand", params: [store_result: true]], this.context)

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

                this.context.drush_command = 'fd ' + feature['feature']
                this.script.drupipeAction("Drush.runCommand", this.context)
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
