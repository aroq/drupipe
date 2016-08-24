package com.github.aroq.workflowlibs

def info(config) {
    echo "Requesting docman for config..."
    sh(
            """#!/bin/bash -l
            if [ "${force}" == "1" ]; then
              FLAG="-f"
              rm -fR docroot
            fi
            docman init docroot ${config.configRepo} -s
            cd docroot
            docman info full config.json
            """
    )
    echo "Requesting docman for config... DONE."
}

def info2(config = null) {
    echo "Requesting docman for config..."
    sh(
            """#!/bin/bash -l
            #docman info full config.json
            """
    )
    echo "Requesting docman for config... DONE."
}

def deploy(config = null) {
    echo "Docman deploy"
    def flag = ''
    if (force == 1) {
        flag = '-f'
    }
    sh """#!/bin/bash -l
       if [ "${force}" == "1" ]; then
         rm -fR docroot
       fi
       docman init docroot ${config_repo} -s
       cd docroot
       docman deploy git_target ${projectName} branch ${version} ${flag}
"""
}
