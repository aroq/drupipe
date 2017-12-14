package com.github.aroq.drupipe.actions

class Git extends BaseAction {

    def clone() {
        script.echo 'Git clone'

        def repoDir = action.params.dir + '/' + action.params.repoDirName

        if (script.fileExists(repoDir)) {
            script.drupipeShell("""
                rm -fR ${repoDir}
                """, action.params
            )
        }

        String options = ''
        if (action.params.singleBranch) {
            options += "-b ${action.params.reference} --single-branch "
        }
        if (action.params.depth) {
            options += "--depth ${action.params.depth}"
        }
        script.drupipeShell("""
            mkdir -p ${action.params.dir}
            cd ${action.params.dir}
            git clone ${options} ${action.params.repoAddress} ${action.params.repoDirName}
            """, action.params
        )
        [:]
    }

}

