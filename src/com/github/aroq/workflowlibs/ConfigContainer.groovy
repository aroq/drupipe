package com.github.aroq.workflowlibs

class ConfigContainer implements Serializable {
    HashMap params = [test4: 'test4']

    def addParams(p) {
//        this.params.param1 = 'test3'
   }

    def getParams() {
        this.params
    }
}