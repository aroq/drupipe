#!groovy

def call(shellCommand, actionParams = [shell_bash_login:true, return_stdout: false]) {
    if (env.KUBERNETES_PORT) {
        echo "Executing ssh with SSH_AUTH_SOCK manually set"
        if (actionParams.shell_bash_login) {
            echo "With bash login session"
            shellCommand = """#!/bin/bash -l -e
                export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}
                ${shellCommand}
                """
        }
        else {
            shellCommand = "export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}; ${shellCommand}"
        }
    }
    else {
        if (actionParams.shell_bash_login) {
            echo "With bash login session"
            shellCommand = """#!/bin/bash -l -e
                ${shellCommand}
                """
        }
    }
    echo "Executing shell command: ${shellCommand} with returnStdout=${actionParams.return_stdout}"
    def result = sh(returnStdout: actionParams.return_stdout, script: "#!/bin/sh -e\n" + shellCommand)
    if (actionParams.return_stdout) {
        echo "Command output: ${result}"
        [stdout: result]
    }
    else {
        [:]
    }
}
