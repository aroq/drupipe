#!groovy

def call(shellCommand, context) {
    if (context.containerMode == 'kubernetes') {
        echo "Executing ssh with SSH_AUTH_SOCK manually set"
        if (context.shellCommandWithBashLogin) {
            echo "With bash login session"
            sh """#!/bin/bash -l
                export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}
                ${shellCommand}
                """
        }
        else {
            sh("export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}; ${shellCommand}")
        }
    }
    else {
        sh(shellCommand)
    }
}

