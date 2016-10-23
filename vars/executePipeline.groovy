def call(body) {
    def params = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

    try {
        _pipelineNotify(params)
        if (params.p) {
            params << params.p
            params.remove('p')
        }

        // Refactor it to retrieve project name from repo address instead of name.
        if (!params.projectName) {
            params.projectName = env.gitlabSourceRepoName
        }

        if (params.noNode) {
            params << _executePipeline(params)
        }
        else {
            node {
                params << _executePipeline(params)
            }
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        _pipelineNotify(params, currentBuild.result)
        params
    }

}

def _executePipeline(params) {
    utils = new com.github.aroq.workflowlibs.Utils()
    stages = [new com.github.aroq.workflowlibs.Stage(name: 'config', actionList: utils.processPipelineActionList([[action: 'Config.perform']]))]

    stages += utils.processPipeline(params.pipeline)
    stages += utils.processStages(params.stages)

    if (jenkinsParam('force') == '1') {
        deleteDir()
    }
    if (params.checkoutSCM) {
        echo 'checkout scm'
        echo "params.checkoutSCM: ${params.checkoutSCM}"
        sh "ls -l"
        checkout scm
    }

    for (int i = 0; i < stages.size(); i++) {
        params.stage = stages[i]
        params << executeStage(stages[i]) {
            p = params
        }
    }
    params
}

def _pipelineNotify(params, String buildStatus = 'STARTED') {
    // build status of null means successful
    buildStatus =  buildStatus ?: 'SUCCESSFUL'

    // Default values
    def colorName = 'RED'
    def colorCode = '#FF0000'
    def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    def summary = "${subject} (${env.BUILD_URL})"
    def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

    // Override default values based on build status
    if (buildStatus == 'STARTED') {
        color = 'YELLOW'
        colorCode = '#FFFF00'
    } else if (buildStatus == 'SUCCESSFUL') {
        color = 'GREEN'
        colorCode = '#00FF00'
    } else {
        color = 'RED'
        colorCode = '#FF0000'
    }

    // Send notifications
    slackSend (color: colorCode, message: summary, channel: params.slackChannel)

    // hipchatSend (color: color, notify: true, message: summary)

    def to = emailextrecipients([
        [$class: 'CulpritsRecipientProvider'],
        [$class: 'DevelopersRecipientProvider'],
        [$class: 'RequesterRecipientProvider']
    ])

//    emailext (
//        subject: subject,
//        body: details,
//        to: to
//    )
}

