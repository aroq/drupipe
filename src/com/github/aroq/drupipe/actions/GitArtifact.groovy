package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class GitArtifact extends BaseAction {

    def script

    def utils

    DrupipeActionWrapper action

    def retrieve() {
        script.drupipeAction([action: "Git.clone", params: action.pipeline.context.builder.artifactParams << action.params], action.pipeline)

        def repoDir = action.params.dir + '/' + action.params.repoDirName

        script.drupipeShell(
            """
                rm -fR ${repoDir}/.git
            """, action.params
        )

        action.pipeline.context.builder.artifactParams.dir = "${action.pipeline.context.workspace}/${action.params.dir}/${action.params.repoDirName}"
    }
}

