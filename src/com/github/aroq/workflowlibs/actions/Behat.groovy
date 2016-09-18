package com.github.aroq.workflowlibs.actions

def perform(params) {
    dir('docroot/master/docroot') {
        if (fileExists('../bin/behat')) {
            try {
                if (fileExists("../code/common/behat.${environment}.yml")) {
                    sh """#!/bin/bash -l
mkdir -p ../../../reports
../bin/behat --config=../code/common/behat.${environment}.yml --format=pretty --out=std --format=junit --out=../../../reports
"""
                }
                else {
                    echo "Behat config file not found: ../code/common/behat.${environment}.yml"
                }
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


