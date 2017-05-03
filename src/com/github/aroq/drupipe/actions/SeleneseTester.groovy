package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class SeleneseTester extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def test() {
        script.drupipeAction([action: "Git.clone", params: action.params], context)

        script.drupipeShell(
        """
        docker run --rm --user root:root -v "${context.workspace}:${context.workspace}" -e "SELENSE_BASE_URL=${action.params.SELENESE_BASE_URL}" -e "SCREEN_WIDTH=1600" -e "SCREEN_HEIGHT=1600" -e "SCREEN_DEPTH=24" --workdir "${context.workspace}/${action.params.dir}/${action.params.repoDirName}" --entrypoint "/opt/bin/entry_point.sh" ${action.params.dockerImage} "${action.params.suiteName}"
        """, context)

    }

}

