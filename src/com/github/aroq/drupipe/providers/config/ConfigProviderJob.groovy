package com.github.aroq.drupipe.providers.config

class ConfigProviderJob extends ConfigProviderBase {

    // TODO: check if this is needed as Config Provider or Processor.
    def provide() {
        utils.log "ConfigProviderJob->provide()"
//        if (drupipeConfig.config.config_version > 1) {
        utils.log "Initialising drupipeProcessorsController"
        controller.drupipeProcessorsController = controller.drupipeConfig.initProcessorsController(this, drupipeConfig.config.processors)
//        }
//        if (drupipeConfig.config.jobs) {
//            controller.archiveObjectJsonAndYaml(drupipeConfig.config, 'context_unprocessed')
//
//            // Performed here as needed later for job processing.
//            controller.drupipeConfig.process()
//
//            drupipeConfig.config.jobs = processJobs(drupipeConfig.config.jobs)
//
//            drupipeConfig.config.job = (drupipeConfig.config.env.JOB_NAME).split('/').drop(1).inject(drupipeConfig.config, { obj, prop ->
//                obj.jobs[prop]
//            })
//
//            if (drupipeConfig.config.job) {
//                if (drupipeConfig.config.job.context) {
//                    drupipeConfig.config = utils.merge(drupipeConfig.config, drupipeConfig.config.job.context)
//                }
//                utils.jsonDump(drupipeConfig.config, drupipeConfig.config.job,'CONFIG JOB', false)
//            }
//            else {
//                throw new Exception("ConfigProviderJob->provide: No job is defined.")
//            }
//        }
//        else {
//            throw new Exception("ConfigProviderJob->provide: No config.jobs are defined")
//        }
       drupipeConfig.config
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
