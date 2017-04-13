package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class GitArtifact extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def retrieve() {
        script.drupipeAction([action: "Git.clone", params: context.builder.artifactParams << action.params], context)
        context.builder.artifactParams.dir = "../../../${action.params.dir}/${action.params.repoDirName}"
        script.drupipeShell("cd docroot/config/ansible; ls -l ${action.params.dir}/${action.params.repoDirName}", context)
    }
}

