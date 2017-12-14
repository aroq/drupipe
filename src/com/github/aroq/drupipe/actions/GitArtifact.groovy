package com.github.aroq.drupipe.actions

class GitArtifact extends BaseAction {

    def retrieve() {
        script.drupipeAction([action: "Git.clone", params: action.pipeline.context.builder.artifactParams << action.params], action.pipeline)

        def repoDir = action.params.dir + '/' + action.params.repoDirName

        script.drupipeShell(
            """
                rm -fR ${repoDir}/.git
            """, action.params
        )

        action.pipeline.context.builder.artifactParams.dir = "${action.params.dir}/${action.params.repoDirName}"
        [:]
    }
}
