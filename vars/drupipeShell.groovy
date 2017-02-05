#!groovy

def call(shellCommand, context) {
    if (context.block.withKubernetes) {
        echo "Executing ssh with SSH_AUTH_SOCK manually set"
        if (context.shellCommandWithBashLogin) {
            sh("""#!/bin/bash -l
               export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK};
               ${shellCommand}
               """)
        }
        else {
            sh("export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK} ${shellCommand}")
        }
    }
    else {
        sh(shellCommand)
    }
}

