package com.github.aroq.workflowlibs.actions

def perform(params) {
    if (params.testEnvironment) {
        testEnvironment = params.testEnvironment
    }
    else {
        testEnvironment = environment
    }
    features = ''
    if (params.features) {
        features = params.features
    }
    tags = ''
    if (params.tags) {
        tags = "--tags=${params.tags}"
    }

    // TODO: Add settings to exit with error on Behat errors.
    if (fileExists("${params.masterPath}/${params.behatExecutable}")) {
        if (fileExists("${params.masterPath}/${params.pathToEnvironmentConfig}/behat.${testEnvironment}.yml")) {
            sh """#!/bin/bash -l
cd ${params.masterPath}/${params.docrootDir}
mkdir -p ${params.workspaceRelativePath}/reports
${params.masterRelativePath}/${params.behatExecutable} --config=${params.masterRelativePath}/${params.pathToEnvironmentConfig}/behat.${testEnvironment}.yml ${params.behat_args} --out=${params.workspaceRelativePath}/reports ${tags} ${features}
"""
        }
        else {
            throw new Exception("Behat config file not found: ${params.masterPath}/${params.pathToEnvironmentConfig}/behat.${testEnvironment}.yml")
        }
    }
    else {
        throw new Exception("Behat execution file doesn't present: ${params.masterPath}/${params.behatExecutable}")
    }
}


