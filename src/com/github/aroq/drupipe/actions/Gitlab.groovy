package com.github.aroq.drupipe.actions

class Gitlab extends BaseAction {

    def acceptMR() {
        script.addGitLabMRComment comment: action.params.message
        script.acceptGitLabMR mergeCommitMessage: action.params.message
    }
}
