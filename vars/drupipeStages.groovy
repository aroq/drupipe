def call(stages, config) {
    try {
        _pipelineNotify(config)
        config << _executeStages(stages, config)
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        _pipelineNotify(config, currentBuild.result)
        config
    }
}

def _executeStages(stagesToExecute, params) {
    utils = new com.github.aroq.drupipe.Utils()

    stages = utils.processStages(stagesToExecute)
    stages += utils.processStages(params.stages)

    if (params.force == '11') {
        echo 'FORCE REMOVE DIR'
        deleteDir()
    }
    if (params.checkoutSCM) {
        echo "params.checkoutSCM: ${params.checkoutSCM}"
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
    def details = """<p>Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>"""

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
    if (params.notificationsSlack) {
        try {
            slackSend (color: colorCode, message: summary, channel: params.slackChannel)
        }
        catch (e) {
            echo 'Unable to sent Slack notification'
        }
    }

    if (params.notificationsMattermost) {
      try {
          mattermostSend (color: colorCode, message: summary, channel: params.mattermostChannel)
      }
      catch (e) {
          echo 'Unable to sent Mattermost notification'
      }
    }

    // hipchatSend (color: color, notify: true, message: summary)

    if (params.notificationsEmailExt) {
      def to = emailextrecipients([
          [$class: 'CulpritsRecipientProvider'],
          [$class: 'DevelopersRecipientProvider'],
          [$class: 'RequesterRecipientProvider']
      ])

      emailext (
          subject: subject,
          body: details,
          to: to,
          mimeType: 'text/html',
          attachLog: true,
      )
    }
}

