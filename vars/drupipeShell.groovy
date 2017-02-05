#!groovy

def call(shellCommand, context) {
    if (true || context.withKubernetes) {
        echo "Executing ssh with SSH_AUTH_SOCK manually set"
        sh("SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK} ${shellCommand}")
    }
    else {
        sh(shellCommand)
    }
}

