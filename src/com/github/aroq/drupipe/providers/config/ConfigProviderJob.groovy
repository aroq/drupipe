package com.github.aroq.drupipe.providers.config

import com.github.aroq.drupipe.processors.DrupipeProcessor
import com.github.aroq.drupipe.processors.DrupipeProcessorsController

class ConfigProviderJob extends ConfigProviderBase {

    def provide() {
        def result = [:]
        if (config.jobs) {
            controller.archiveObjectJsonAndYaml(config, 'context_unprocessed')

            // Performed here as needed later for job processing.
            controller.drupipeConfig.process()

            utils.log "AFTER jobConfig() controller.drupipeConfig.process()"

            utils.jsonDump(config, config.jobs, 'CONFIG JOBS PROCESSED - BEFORE processJobs', false)

            config.jobs = processJobs(config.jobs)

            utils.jsonDump(config, config.jobs, 'CONFIG JOBS PROCESSED - AFTER processJobs', false)

            result.job = (config.env.JOB_NAME).split('/').drop(1).inject(config, { obj, prop ->
                obj.jobs[prop]
            })

            if (result.job) {
                if (result.job.context) {
                    result = utils.merge(result, result.job.context)
                }
            }
        }
        else {
            utils.log "Config.jobConfig() -> No config.jobs are defined"
        }
        result
    }

    def processJobs(jobs, parentParams = [:]) {
        def result = jobs
        for (job in jobs) {
            // For compatibility with previous config versions.
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
        result
    }

}
