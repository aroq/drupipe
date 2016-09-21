import groovy.json.JsonSlurper

projects = JsonSlurper.newInstance().parseText(readFileFromWorkspace('projects.json')).projects

projects.each {project ->
    pipeline = ''

    folder(project.key)

    pipelineJob("${project.key}/seed") {
        concurrentBuild(false)
        logRotator(-1, 30)
        parameters {
            stringParam('debug', '0')
        }
        definition {
            cpsScm {
                scm {
                    git(project.value['repo'])
                }
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

