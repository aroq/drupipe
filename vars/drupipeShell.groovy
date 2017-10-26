#!groovy

def call(shellCommand, context, actionParams = [:]) {
    if (context.containerMode == 'kubernetes') {
        echo "Executing ssh with SSH_AUTH_SOCK manually set"
        if (actionParams.shellCommandWithBashLogin) {
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
        if (actionParams.shellCommandWithBashLogin) {
            echo "With bash login session"
            shellCommand = """#!/bin/bash -l
                ${shellCommand}
                """
        }
    }
    echo "Executing shell command: ${shellCommand} with returnStdout=${actionParams.drupipeShellReturnStdout}"
    context.drupipeShellResult = sh(returnStdout: actionParams.drupipeShellReturnStdout, script: shellCommand)
    if (actionParams.drupipeShellReturnStdout) {
        echo "Command output: ${context.drupipeShellResult}"
        [drupipeShellResult: context.drupipeShellResult]
    }
    else {
        []
    }
}
