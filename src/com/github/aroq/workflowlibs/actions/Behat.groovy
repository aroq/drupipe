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

//    dir('docroot/master/docroot') {
    sh('ls -al')
    sh('ls docroot -al')
    sh('ls docroot/master -al')
    sh('ls docroot/bin/behat -al')
        if (fileExists('docroot/bin/behat')) {
            if (fileExists("docroot/code/common/behat.${testEnvironment}.yml")) {
                sh """#!/bin/bash -l
cd docroot/master/docroot
mkdir -p reports
docroot/bin/behat --config=docroot/code/common/behat.${testEnvironment}.yml --format=pretty --out=std --format=junit --out=reports ${tags} ${features}
"""
            }
            else {
                echo "Behat config file not found: docroot/code/common/behat.${testEnvironment}.yml"
            }
        }
        else {
            echo "Behat execution file doesn't present"
        }
//    }
}


