package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Gitlab extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def acceptMR() {
        script.addGitLabMRComment comment: action.params.message
        script.acceptGitLabMR mergeCommitMessage: action.params.message
    }
}
