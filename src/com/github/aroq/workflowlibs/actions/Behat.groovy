package com.github.aroq.workflowlibs.actions

def perform(params) {
    dir('docroot/master/docroot') {
        if (fileExists('../bin/behat') && fileExists("../code/common/behat.${environment}.yml")) {
            try {
                sh """#!/bin/bash -l
mkdir -p ../../../reports
../bin/behat --config=../code/common/behat.${environment}.yml --format=pretty --out=std --format=junit --out=../../../reports
"""
            }
            catch (err) {
                currentBuild.result = 'UNSTABLE'
            }
        }
        else {
            echo "Behat execution file doesn't present"
        }
    }
}


