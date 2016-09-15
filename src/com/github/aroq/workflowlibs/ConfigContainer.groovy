package com.github.aroq.workflowlibs

class ConfigContainer implements Serializable {
    HashMap params = [:]

    def addParams(p) {
        this.params = [test: 'test']
   }

    def getParams() {
        this.params
    }
}