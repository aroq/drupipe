#!groovy

def call(shellCommand, context) {
    if (context.containerMode == 'kubernetes') {
        echo "Executing ssh with SSH_AUTH_SOCK manually set"
        if (context.shellCommandWithBashLogin) {
            echo "With bash login session"
            shellCommand = """#!/bin/bash -l
                export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}
                ${shellCommand}
                """
        }
        else {
            shellCommand = "export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}; ${shellCommand}"
        }
    }
    else {
        if (context.shellCommandWithBashLogin) {
            echo "With bash login session"
            shellCommand = """#!/bin/bash -l
                ${shellCommand}
                """
        }
    }
    echo "Executing shell command: ${shellCommand}"
    context.drupipeShellResult = sh(returnStdout: context.drupipeShellReturnStdout, script: shellCommand)
    //echo context.drupipeShellResult
}
