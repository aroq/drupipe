#!groovy

def call(shellCommand, context, actionParams = [:]) {
    def params = [:]
    params << context << actionParams
    if (params.containerMode == 'kubernetes') {
        echo "Executing ssh with SSH_AUTH_SOCK manually set"
        if (params.shellCommandWithBashLogin) {
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
        if (params.shellCommandWithBashLogin) {
            echo "With bash login session"
            shellCommand = """#!/bin/bash -l
                ${shellCommand}
                """
        }
    }
    echo "Executing shell command: ${shellCommand} with returnStdout=${params.drupipeShellReturnStdout}"
    context.drupipeShellResult = sh(returnStdout: params.drupipeShellReturnStdout, script: shellCommand)
    if (params.drupipeShellReturnStdout) {
        echo "Command output: ${context.drupipeShellResult}"
        [drupipeShellResult: context.drupipeShellResult]
    }
    else {
        []
    }
}
