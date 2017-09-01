def call(context = [:], body) {

    podTemplate(label: 'mypod', containers: [
        containerTemplate(name: 'docman', image: 'michaeltigr/zebra-build-php-drush-docman', ttyEnabled: true, command: 'cat'),
    ]) {
        node('mypod') {
            container('docman') {
                unstash('config')
                context << context.defaultActionParams['drupipeWithKubernetes'] << context
                sh('drush --version')
                context.workspace = pwd()
                drupipeShell("echo 'test'", context)
                sshagent([context.credentialsId]) {
//            drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", context)
                    result = body(context)
                    if (result) {
                        context << result
                    }
                }
            }
        }
    }

//    nodeName = 'drupipe'
//    podTemplate(label: 'mypod', containers: [
//        containerTemplate(name: context.containerName, image: context.dockerImage, ttyEnabled: true, command: 'cat'),
//    ]) {
//        node('mypod') {
//            echo "NODE NAME: ${nodeName}"
//            container(context.containerName) {
//            }
//        }
//    }


    context
}
