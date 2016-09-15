package com.github.aroq.workflowlibs

class ConfigContainer implements Serializable {
    HashMap params = [:]

    String addParams(params) {
        this.params << params
    }

    def getParams() {
        this.params
    }
}