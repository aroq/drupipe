package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionConroller

class GitArtifact extends BaseAction {

    def context

    def script

    def utils

    def DrupipeActionConroller action

    def retrieve() {
        script.drupipeAction([action: "Git.clone", params: context.builder.artifactParams << action.params], context)

        def repoDir = action.params.dir + '/' + action.params.repoDirName

        script.drupipeShell(
            """
                rm -fR ${repoDir}/.git
            """, action.params
        )

        context.builder.artifactParams.dir = "${context.workspace}/${action.params.dir}/${action.params.repoDirName}"
    }
}

