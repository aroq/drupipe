package com.github.aroq.dsl

import groovy.json.JsonSlurper

class DslHelper {

    def script

    def config

    def readJson(script, file) {
        JsonSlurper.newInstance().parseText(script, readFileFromWorkspace(file))
    }

    Map merge(Map[] sources) {
        if (sources.length == 0) return [:]
        if (sources.length == 1) return sources[0]

        sources.inject([:]) { result, source ->
            if (source && source.containsKey('override') && source['override']) {
                result = source
            }
            else {
                source.each { k, v ->
                    if (result[k] instanceof Map && v instanceof Map ) {
                        if (v.containsKey('override') && v['override']) {
                            v.remove('override')
                            result[k] = v
                        }
                        else {
                            result[k] = merge(result[k], v)
                        }
                    }
                    else if (result[k] instanceof List && v instanceof List) {
                        result[k] += v
                        result[k] = result[k].unique()
                    }
                    else {
                        result[k] = v
                    }
                }
            }
            result
        }
    }

    def sourcePath(params, sourceName, String path) {
        if (sourceName in params.loadedSources) {
            println "sourcePath: " + params.loadedSources[sourceName].path + '/' + path
            params.loadedSources[sourceName].path + '/' + path
        }
    }

    def sourceDir(params, sourceName) {
        if (sourceName in params.loadedSources) {
            println "sourceDir: " + params.loadedSources[sourceName].path
            params.loadedSources[sourceName].path
        }
    }

    def getServersByTags(tags, servers) {
        def result = [:]
        if (tags && tags instanceof ArrayList) {
            for (def i = 0; i < tags.size(); i++) {
                def tag = tags[i]
                for (server in servers) {
                    if (server.value?.tags && tag in server.value?.tags && server.value?.jenkinsUrl) {
                        result << ["${server.key}": server.value]
                    }
                }
            }
        }
        println "getServersByTags: ${result}"
        result
    }
}
