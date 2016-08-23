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

