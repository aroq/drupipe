package com.github.aroq.dsl

import groovy.json.JsonSlurper

/**
 * Created by Aroq on 06/06/16.
 */
class DocmanConfig {

    def docrootConfigJson

    def docmanConfig

    def script

    def init() {
        docmanConfig = JsonSlurper.newInstance().parseText(docrootConfigJson)
    }

    def getProjects() {
        init()
        docmanConfig['projects']
    }

    def getStates() {
        init()
        docmanConfig['states']
    }

    def getEnvironmentByState(String stateName) {
        def states = getStates()
        states[stateName]
    }

    def getEnvironments() {
        init()
        docmanConfig['environments']
    }

    def getType(project = 'master') {
        init()
        if (docmanConfig.projects[project]['type']) {
            return docmanConfig.projects[project]['type']
        }
        else {
            throw new RuntimeException("There is no project ${project} defined in projects")
        }
    }

    def getVersionBranch(project, stateName) {
        init()
        if (!project) {
            // TODO: retrieve first project from docman config.
            def projectMap = docmanConfig.projects.find { it.value.containsKey('states') }
            if (projectMap) {
                project = projectMap.key
                script.println "First project name: ${project}"
            }
            else {
                throw new RuntimeException("Project with states is not found in ${docmanConfig.projects}")
            }

        }
        if (docmanConfig.projects[project]['states'][stateName]) {
            if (docmanConfig.projects[project]['states'][stateName]['version']) {
                docmanConfig.projects[project]['states'][stateName]['version']
            }
            else if (docmanConfig.projects[project]['states'][stateName]['source']) {
                docmanConfig.projects[project]['states'][stateName]['source']['branch']
            }
        }
        else {
            throw new RuntimeException("There is no state ${stateName} defined in project ${docmanConfig.projects[project]}")
        }
    }

}
