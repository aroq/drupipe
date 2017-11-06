import com.github.aroq.drupipe.DrupipeBlock

def call(pipeline, body) {
    echo "Container mode: kubernetes"
    def nodeName = 'drupipe'
    def containers = []

    ArrayList blocks = pipeline.blocks

    ArrayList masterBlocks = []

    for (def i = 0; i < blocks.size(); i++) {
        echo "BLOCK name: ${blocks[i].name}"
        echo "BLOCK dockerImage: ${blocks[i].dockerImage}"
        echo "BLOCK nodeName: ${blocks[i].nodeName}"

        if (blocks[i].withDocker) {
            echo "Create k8s containerTemplate for block: ${blocks[i].name}, image: ${blocks[i].dockerImage}"
            containers.add(containerTemplate(
                name: "block${i}",
                image: blocks[i].dockerImage,
                ttyEnabled: true,
                command: 'cat',
                // TODO: Re-check it.
                resourceRequestCpu: '50m',
                resourceLimitCpu: '100m',
//                resourceRequestMemory: '100Mi',
//                resourceLimitMemory: '200Mi',
                alwaysPullImage: true,
                envVars: [
                    envVar(key: 'TF_VAR_consul_address', value: pipeline.context.env.TF_VAR_consul_address),
                    secretEnvVar(key: 'DIGITALOCEAN_TOKEN', secretName: 'zebra-keys', secretKey: 'zebra_do_token'),
//                  secretEnvVar(key: 'CONSUL_ACCESS_TOKEN', secretName: 'zebra-keys', secretKey: 'zebra_consul_access_token'),
                    secretEnvVar(key: 'ANSIBLE_VAULT_PASS_FILE', secretName: 'zebra-keys', secretKey: 'zebra_ansible_vault_pass'),
                    secretEnvVar(key: 'GITLAB_API_TOKEN_TEXT', secretName: 'zebra-keys', secretKey: 'zebra_gitlab_api_token'),
                ],
            ))
        }
        else {
            masterBlocks += blocks[i]
        }
    }

    def creds = [string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
    withCredentials(creds) {
        podTemplate(
            label: nodeName,
            containers: containers
        ) {
            node(nodeName) {
                pipeline.context.workspace = pwd()
                for (def i = 0; i < blocks.size(); i++) {
                    blocks[i].name = "block${i}"
                    blocks[i].pipeline = pipeline
                    if (blocks[i].withDocker) {
                        container("block${i}") {
                            pipeline.scmCheckout()
                            unstash('config')
                            DrupipeBlock block = new DrupipeBlock(blocks[i])
                            echo 'BLOCK EXECUTE START'
                            sshagent([pipeline.context.credentialsId]) {
                                block.blockInNode = true
                                block.execute()
                            }
                            echo 'BLOCK EXECUTE END'
                        }
                    }
                }
            }
        }
    }

    node("master") {
        for (def i = 0; i < masterBlocks.size(); i++) {

//        blocks[i].name = "block${i}"
            masterBlocks[i].pipeline = pipeline
            pipeline.scmCheckout()
            unstash('config')
            def block = new DrupipeBlock(masterBlocks[i])
            echo 'BLOCK EXECUTE START'
            sshagent([pipeline.context.credentialsId]) {
                block.execute()
            }
            echo 'BLOCK EXECUTE END'
        }
    }
}

