package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Helm extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def config() {
        this.script.echo "Helm.config"
    }

    def status() {
        this.script.echo "Helm.status"
    }

    def install() {
        this.script.echo "Helm.install"
    }

    def delete() {
        this.script.echo "Helm.delete"
    }

}

