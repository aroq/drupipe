package com.github.aroq.drupipe.actions

import groovy.json.JsonSlurperClassic
import com.github.aroq.drupipe.DrupipeAction

class DrushFeaturesList extends BaseAction {

    def context

    def script

    def utils

    String state = 'Overridden'

    HashMap notification = [:]

    def DrupipeAction action

    def runCommand() {
        def features = []

        this.notification.level = "action:${this.context.drupipeStageName}"
        this.notification.status = 'INFO'

        this.context.drush_command = 'fl --format=json'
        this.script.drupipeAction("Drush.runCommand", this.context.clone() << [drushOutputReturn: 1])

        def output = this.context.lastActionOutput
        def jsonOutput = new JsonSlurperClassic().parseText(output)
        jsonOutput.each { key, feature->
            if (feature.containsKey('state') && feature['state'] == this.state) {
                features << feature
            }
        }

        if (features.size() > 0) {
            def exception_table = "|Name|Machine name|\n|:---|:---|\n"
            features.each { feature ->
                exception_table = exception_table + "|${feature['name']}|${feature['feature']}|"

                this.context.drush_command = 'fd ' + feature['feature']
                this.script.drupipeAction("Drush.runCommand", this.context.clone() << [drushOutputReturn: 1])

                def diff = this.context.lastActionOutput
                diff = diff.substring(diff.indexOf('Legend:'))

                this.notification.message = '```' + diff + '```'

                this.notification.name = "**${feature['name']}** (${feature['feature']})"

                this.utils.pipelineNotify(this.context, this.notification)
            }

            this.context.lastActionOutput

            throw new Exception("OVERRIDEN FEATURES:\n\n${exception_table}")
        }
        else {
            this.script.echo "DRUSH FEATURES LIST: No overridden features."
            this.context.lastActionOutput "No overridden features."
        }

        features
    }
}
