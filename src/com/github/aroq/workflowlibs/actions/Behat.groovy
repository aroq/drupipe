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

    dir('docroot/master/docroot') {
        if (fileExists('../bin/behat')) {
            if (fileExists("../code/common/behat.${testEnvironment}.yml")) {
                sh """#!/bin/bash -l
mkdir -p ../../../reports
../bin/behat --config=../code/common/behat.${testEnvironment}.yml --format=pretty --out=std --format=junit --out=../../../reports ${features}
"""
            }
            else {
                echo "Behat config file not found: ../code/common/behat.${testEnvironment}.yml"
            }
        }
        else {
            echo "Behat execution file doesn't present"
        }
    }
}


