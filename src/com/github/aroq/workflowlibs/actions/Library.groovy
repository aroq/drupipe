package com.github.aroq.workflowlibs.actions

def perform(params) {
    dir('library') {
        git url: 'https://github.com/aroq/jenkins-pipeline-library.git', branch: 'master'
    }
}

