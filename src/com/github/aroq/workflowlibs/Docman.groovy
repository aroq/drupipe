package com.github.aroq.workflowlibs

def info(params = [:]) {
    def config = configVault.get
    dump(config)

    echo "Requesting docman for config..."
    if (force == 1) {
        sh(
            """#!/bin/bash -l
            if [ "${force}" == "1" ]; then
              FLAG="-f"
              rm -fR docroot
            fi
            """
        )
    }
    if (config.initDocman && config.configRepo) {
        sh(
            """#!/bin/bash -l
            docman init docroot ${config.configRepo} -s
            """
        )
    }
    sh(
        """#!/bin/bash -l
        cd docroot
        docman info full config.json
        """
    )
    echo "Requesting docman for config... DONE."
}

def deploy(config = [:]) {
    echo "Docman deploy"
    def flag = ''
    if (force == 1) {
        flag = '-f'
    }
    sh(
        """#!/bin/bash -l
        if [ "${force}" == "1" ]; then
          rm -fR docroot
        fi
        docman init docroot ${config_repo} -s
        cd docroot
        docman deploy git_target ${projectName} branch ${version} ${flag}
        """
    )
}
