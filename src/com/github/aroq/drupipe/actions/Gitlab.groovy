package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Gitlab extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    String terraformExecutable = 'terraform'

    def acceptMR() {
        this.script.acceptGitLabMR mergeCommitMessage: 'Pipeline executed successfully.'
    }
}
