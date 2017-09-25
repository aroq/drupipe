import com.github.aroq.drupipe.DrupipeBlock

def call(context = [:], body) {
    echo "Container mode: kubernetes"
    def nodeName = 'drupipe'
    def containers = []

    def blocks = context.pipeline.blocks

    for (def i = 0; i < blocks.size(); i++) {
        if (blocks[i].withDocker) {
            echo "Create k8s containerTemplate for block: ${blocks[i].name}, image: ${blocks[i].dockerImage}"
            containers.add(containerTemplate(
                name: "block${i}",
                image: blocks[i].dockerImage,
                ttyEnabled: true,
                command: 'cat',
                alwaysPullImage: true,
                envVars: [
                    envVar(key: 'TF_VAR_consul_address', value: context.env.TF_VAR_consul_address),
                    secretEnvVar(key: 'DIGITALOCEAN_TOKEN', secretName: 'zebra-keys', secretKey: 'zebra_do_token'),
//                    secretEnvVar(key: 'CONSUL_ACCESS_TOKEN', secretName: 'zebra-keys', secretKey: 'zebra_consul_access_token'),
                    secretEnvVar(key: 'ANSIBLE_VAULT_PASS_FILE', secretName: 'zebra-keys', secretKey: 'zebra_ansible_vault_pass'),
                    secretEnvVar(key: 'GITLAB_API_TOKEN_TEXT', secretName: 'zebra-keys', secretKey: 'zebra_gitlab_api_token'),
                ],
            ))
        }
    }

    def creds = [string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
    withCredentials(creds) {
        podTemplate(
            label: nodeName,
            containers: containers
        ) {
            node(nodeName) {
                for (def i = 0; i < blocks.size(); i++) {
                    blocks[i].name = "block${i}"
                    if (blocks[i].withDocker) {
                        container("block${i}") {
                            context.pipeline.scmCheckout()
                            unstash('config')
                            def block = new DrupipeBlock(blocks[i])
                            echo 'BLOCK EXECUTE START'
                            sshagent([context.credentialsId]) {
                                block.blockInNode = true
                                context << block.execute(context)
                            }
                            echo 'BLOCK EXECUTE END'
                        }
                    }
                    else {
                        context.pipeline.scmCheckout()
                        unstash('config')
                        def block = new DrupipeBlock(blocks[i])
                        echo 'BLOCK EXECUTE START'
                        sshagent([context.credentialsId]) {
                            context << block.execute(context)
                        }
                        echo 'BLOCK EXECUTE END'
                    }
                }
            }
        }
    }

    context
}
