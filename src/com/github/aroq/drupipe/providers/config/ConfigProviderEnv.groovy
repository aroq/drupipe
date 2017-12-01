package com.github.aroq.drupipe.providers.config

class ConfigProviderEnv extends ConfigProviderBase {

    def provide() {
        def result = [:]
        result.workspace = script.pwd()
        result.env = utils.envToMap()

        // TODO: Use env vars pattern to override.
        result.credentialsId = result.env.credentialsId
        result.environment = result.env.environment
        result.configRepo = result.env.configRepo

        String jobPath = script.env.BUILD_URL ? script.env.BUILD_URL : script.env.JOB_DISPLAY_URL
        result.jenkinsFolderName = utils.getJenkinsFolderName(jobPath)
        result.jenkinsJobName = utils.getJenkinsJobName(jobPath)

        if (script.env.KUBERNETES_PORT) {
          result.containerMode = 'kubernetes'
        }
        utils.serializeAndDeserialize(result)
    }

}
