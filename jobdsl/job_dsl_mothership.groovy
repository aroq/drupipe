import groovy.json.JsonSlurper

projects = JsonSlurper.newInstance().parseText(readFileFromWorkspace('projects.json'))

projects.each {project ->
    pipeline = ''

    folder(project)

    pipelineJob("${project}/seed") {
        concurrentBuild(false)
        logRotator(-1, 30)
        parameters {
            stringParam('debug', '0')
            stringParam('force', '0')
//            stringParam('type', project.value['type'])
        }
        definition {
            cps {
                script(pipeline)
                sandbox()
            }
        }
        triggers {
            gitlabPush {
                buildOnPushEvents()
                buildOnMergeRequestEvents(false)
                enableCiSkip()
                useCiFeatures()
                includeBranches('master')
            }
        }
        properties {
            gitLabConnectionProperty {
                gitLabConnection('Gitlab')
            }
        }
    }
}

