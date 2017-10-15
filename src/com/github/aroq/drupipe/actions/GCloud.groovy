package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class GCloud extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def auth() {
        this.script.echo "gcloud.auth"
    }

}

