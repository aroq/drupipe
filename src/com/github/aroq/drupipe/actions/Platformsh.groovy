package com.github.aroq.drupipe.actions

class Platformsh extends BaseAction {

    def execute() {
        action.pipeline.drupipeLogger.debugLog(action.pipeline.context, action.params, "ACTION.PARAMS", [debugMode: 'json'])
        if (action.params.repo && action.params.branch) {
            script.echo "Platform.sh repo: ${action.params.repo}"
            script.drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", action.params)
            cleanup()
            getPlatformShRepo()
            syncCode()
            pushCode()
        }
        else {
            action.pipeline.drupipeLogger.error "Platform.sh repo or branch are not defined"
        }
    }

    def cleanup() {
        def command = """
rm -fR ${action.params.platformShDir}
mkdir -p ${action.params.platformShDir}
"""
        script.echo "Cleanup deploy workspace"
        script.drupipeShell(command, action.params)
    }

    def getPlatformShRepo() {
        def command = """
cd ${action.params.platformShDir}
git init .
git remote add origin ${action.params.repo}
git fetch origin ${action.params.branch}
git checkout ${action.params.branch}
"""
        script.echo "Get Platform.sh repo"
        script.drupipeShell(command, action.params)
    }

    def syncCode() {
        def command = """
cd ${action.params.platformShDir}
find . -mindepth 1 -maxdepth 1 -not -name '.git' -print0 | xargs -r -0 rm -rf --
rsync -a --exclude '.git' --delete ${action.pipeline.context.workspace}/${action.pipeline.context.builder.artifactParams.dir}/ ${action.pipeline.context.workspace}/${action.params.platformShDir}/
"""
        script.echo "Update code"
        script.drupipeShell(command, action.params)
    }

    def pushCode() {
        def command = """
cd ${action.params.platformShDir}
git add --all
git commit --quiet --allow-empty -m 'Deploy ${action.pipeline.context.builder.artifactParams.reference} version to ${action.params.branch} environment.'
git push origin ${action.params.branch}
"""
        script.echo "Push code"
        script.drupipeShell(command, action.params)
    }

}
