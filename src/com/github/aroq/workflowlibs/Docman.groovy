package com.github.aroq.workflowlibs

def info(params) {
    echo "Requesting docman for config..."
    sh(
            """#!/bin/bash -l
            if [ "${force}" == "1" ]; then
              FLAG="-f"
              rm -fR docroot
            fi
            docman init docroot ${params.configRepo} -s
            cd docroot
            docman info full config.json
            """
    )
    echo "Requesting docman for config... DONE."
}

def info2(params = nil) {
    echo "Requesting docman for config..."
    sh(
            """#!/bin/bash -l
            #docman info full config.json
            """
    )
    echo "Requesting docman for config... DONE."
}

def deploy(params = nil) {
    echo "Docman deploy"
}
