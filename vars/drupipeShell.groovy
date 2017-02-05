#!groovy

def call(shellCommand, context) {
    if (context.withKubernetes) {
        sh("SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK} ${shellCommand}")
    }
    else {
        sh(shellCommand)
    }
}

