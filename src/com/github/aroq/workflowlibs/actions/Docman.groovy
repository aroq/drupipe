package com.github.aroq.workflowlibs.actions

def info(params) {
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
    if (params.configRepo) {
        configRepo = params.configRepo
    }
    if (config_repo) {
        configRepo = config_repo
    }
    if (configRepo) {
        sh(
            """#!/bin/bash -l
            docman init docroot ${configRepo} -s
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

def deploy(params) {
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

def init(params) {
    echo "Docman init"
    def flag = ''
    if (force == 1) {
        flag = '-f'
    }
    if (params.configRepo) {
        configRepo = params.configRepo
    }
    if (config_repo) {
        configRepo = config_repo
    }
    if (configRepo) {
        sh(
            """#!/bin/bash -l
            docman init ${params.path} ${configRepo} -s
            """
        )
        params.dir
    }
    else {
        null
    }
}
