package com.github.aroq.workflowlibs.actions

def perform(params) {
    echo "TEST"
    sh('ls -al')
    dir('library') {
        git url: 'https://github.com/aroq/jenkins-pipeline-library.git', branch: 'master'
    }
    echo "TEST2"
    sh('ls -al')
}

