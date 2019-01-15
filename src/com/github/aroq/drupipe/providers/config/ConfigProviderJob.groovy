package com.github.aroq.drupipe.providers.config

class ConfigProviderJob extends ConfigProviderBase {

    // TODO: check if this is needed as Config Provider or Processor.
    def provide() {
        controller.drupipeLogger.log "ConfigProviderJob->provide()"
//        if (drupipeConfig.config.config_version > 1) {
        controller.drupipeLogger.log "Initialising drupipeProcessorsController"
        controller.drupipeProcessorsController = controller.drupipeConfig.initProcessorsController(this, drupipeConfig.config.processors)

//        String prefixPath = script.env.JENKINS_HOME + "/config_cache/" + script.env.JOB_NAME
        String prefixPath = script.env.JENKINS_HOME + "/config_cache/cicd-k8s-infra/seed"
//        String jobConfigFileName = prefixPath + "/ConfigProviderJob.yaml"
        def sourceDir = drupipeConfig.drupipeSourcesController.sourceDir(drupipeConfig.config, 'project')
        String jobConfigFileName = prefixPath + "/scenarios/test/ConfigProviderJob.yaml"

        if (this.script.fileExists(jobConfigFileName)) {
            script.echo "Cached ConfigProviderJob is found, loading..."
            return script.readYaml(file: jobConfigFileName)
        }
        else {
            script.echo "Cached ConfigProviderJob is not found: " + jobConfigFileName
        }

//        }
        script.lock('ConfigProviderJob') {
            if (drupipeConfig.config.jobs) {
                controller.archiveObjectJsonAndYaml(drupipeConfig.config, 'context_unprocessed')

                String jobName = drupipeConfig.config.env.JOB_NAME != 'persistent/mothership' ? drupipeConfig.config.jenkinsJobName : 'mothership'

                if (jobName == 'mothership') {
                    drupipeConfig.config.config_version = 2
                }
                // Performed here as needed later for job processing.
                controller.drupipeConfig.process()

                drupipeConfig.config.jobs = processJobs(drupipeConfig.config.jobs)

                if (jobName.contains('/')) {
                    drupipeConfig.config.job = jobName.tokenize('/').inject(drupipeConfig.config, { obj, prop ->
                        obj.jobs[prop]
                    })
                } else {
                    drupipeConfig.config.job = drupipeConfig.config.jobs[jobName]
                    //                drupipeConfig.config.config_version = 2
                }

                if (drupipeConfig.config.job) {
                    if (drupipeConfig.config.job.context) {
                        drupipeConfig.config = utils.merge(drupipeConfig.config, drupipeConfig.config.job.context)
                    }
                    controller.drupipeLogger.jsonDump(drupipeConfig.config.job, 'CONFIG JOB')
                } else {
                    throw new Exception("ConfigProviderJob->provide: No job is defined.")
                }
            } else {
                throw new Exception("ConfigProviderJob->provide: No config.jobs are defined")
            }
//            controller.archiveObjectJsonAndYaml(drupipeConfig.config, 'ConfigProviderJob')
            script.sh("mkdir -p ${prefixPath}")
            controller.archiveObject(jobConfigFileName, drupipeConfig.config)
        }
        drupipeConfig.config
    }

    def processJobs(jobs, parentParams = [:]) {
        def result = jobs
        for (job in jobs) {
            // For compatibility with previous config versions.
            if (job.value) {
                if (job.value.children) {
                    job.value.jobs = job.value.remove('children')
                }
                if (job.value.jobs) {
                    def params = job.value.clone()
                    params.remove('jobs')
                    job.value.jobs = processJobs(job.value.jobs, params)
                }
                if (parentParams) {
                    result[job.key] = utils.merge(parentParams, job.value)
                }
                else {
                    result[job.key] = job.value
                }
            }
        }
        result
    }

}
