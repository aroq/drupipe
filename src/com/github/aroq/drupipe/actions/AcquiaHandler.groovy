package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class AcquiaHandler extends BaseAction {

    def context

    def script

    def utils

    def DrupipeActionWrapper action


    def deploy() {
        script.drupipeAction([action: 'Druflow.deploy', params: this.action.params], context)
    }

}
