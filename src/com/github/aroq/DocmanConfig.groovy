package com.github.aroq

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

    def getEnvironments() {
        init()
        docmanConfig['environments']
    }

    def getVersionBranch(project, stateName) {
        script.println "DocmanConfig test"
        init()
        if (docmanConfig.projects[project]['states'][stateName]['version']) {
            docmanConfig.projects[project]['states'][stateName]['version']
        }
        else if (docmanConfig.projects[project]['states'][stateName]['source']) {
            docmanConfig.projects[project]['states'][stateName]['source']['branch']
        }
    }

}
