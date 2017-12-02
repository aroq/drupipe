package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionWrapper

class Config extends BaseAction {

    def script

    com.github.aroq.drupipe.Utils utils

    DrupipeActionWrapper action

    def configRepo

    def perform() {
        if (action.pipeline.context['Config_perform']) {
            return
        }




        def providers = [
//            [
//                action: 'GroovyFileConfig.groovyConfigFromLibraryResource',
//                params: [
//                    // We need to pass params as "context" is not ready yet.
//                    store_result: true,
//                    interpolate: false,
//                    post_process: [
//                        context: [
//                            type: 'result',
//                            source: 'result',
//                            destination: 'context',
//                        ],
//                    ],
//                    resource: 'com/github/aroq/drupipe/config.groovy'
//                ]
//            ],
            [
                action: "Config.envConfig",
            ],
            [
                action: "Config.mothershipConfig",
            ],
            [
                action: "Config.config"
            ],
            [
                action: "Config.projectConfig"
            ],
            [
                action: "Config.jenkinsConfig"
            ],
            [
                action: "Config.jobConfig"
            ],
        ]

        if (action.pipeline.context.configProviders) {
            providers << action.pipeline.context.configProviders
        }

        [:]
    }


}
