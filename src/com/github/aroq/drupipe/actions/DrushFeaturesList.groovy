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
        def fl_result = this.script.drupipeAction("Drush.runCommand", this.context.clone() << [drushOutputReturn: 1])

        def jsonOutput = new JsonSlurperClassic().parseText(fl_result.drupipeShellResult)
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
                def fd_result = this.script.drupipeAction("Drush.runCommand", this.context.clone() << [drushOutputReturn: 1])

                fd_result = fd_result.drupipeShellResult.substring(fd_result.drupipeShellResult.indexOf('Legend:'))

                this.notification.message = '```' + fd_result + '```'

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
