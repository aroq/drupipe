#!groovy

def call(shellCommand, context) {
    if (context.block.withKubernetes) {
        echo "Executing ssh with SSH_AUTH_SOCK manually set"
        if (context.shellCommandWithBashLogin) {
            echo "With bash login session"
            sh """#!/bin/bash -l
                export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}
                docman init test git@code.adyax.com:CI-Sample-Multirepo/config-dev.git
                ${shellCommand}
                """
        }
        else {
            sh("export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK} ${shellCommand}")
        }
    }
    else {
        sh(shellCommand)
    }
}

