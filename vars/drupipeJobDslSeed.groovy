#!groovy

def call(body) {
    echo "Params: ${params}"
    def commandParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = commandParams
    body()
    if (commandParams.commandParams) {
        commandParams << commandParams.commandParams
        commandParams.remove('commandParams')
    }

    // Pipeline used to create project specific pipelines.
    withDrupipeDocker() {
        commandParams << it
        parameters = executePipelineAction(action: 'Docman.info', commandParams)

        stash name: 'config', includes: 'docroot/config/**, library/**, mothership/**', excludes: '.git, .git/**'
        parameters
    }

    node('master') {
        if (fileExists('docroot/config')) {
            dir('docroot/config') {
                deleteDir()
            }
            dir('library') {
                deleteDir()
            }
            dir('mothership') {
                deleteDir()
            }
        }

        unstash 'config'
        parameters.actioncommandParams.JobDslSeed_perform.jobsPattern << 'docroot/config/pipelines/jobdsl/*.groovy'
        executePipelineAction(action: 'JobDslSeed.perform', parameters)
    }

}
