import com.github.aroq.drupipe.DrupipeContainerBlock
import com.github.aroq.drupipe.DrupipeController

def call(ArrayList containers, DrupipeController controller) {
    echo "Container mode: kubernetes"
    def nodeName = 'drupipe'
    def containerNames = []
    def containersToExecute= []

    for (def i = 0; i < containers.size(); i++) {
        def container = containers[i]
        def containerName = container.name.replaceAll('\\.','-').replaceAll('_','-')
        if (!containerNames.contains(containerName)) {
            echo "Create k8s containerTemplate for container: ${container.name}, image: ${container.image}"
            containerNames += containerName
            containersToExecute.add(containerTemplate(
                name: containerName,
                image: container.image,
                ttyEnabled: true,
                command: 'cat',
                // TODO: Re-check it.
                resourceRequestCpu: '50m',
//            resourceLimitCpu: '100m',
//                resourceRequestMemory: '100Mi',
//                resourceLimitMemory: '200Mi',
                alwaysPullImage: true,
                // TODO: move in configs.
                envVars: [
                    envVar(key: 'TF_VAR_consul_address', value: controller.context.env.TF_VAR_consul_address),
                    secretEnvVar(key: 'DIGITALOCEAN_TOKEN', secretName: 'zebra-keys', secretKey: 'zebra_do_token'),
//                  secretEnvVar(key: 'CONSUL_ACCESS_TOKEN', secretName: 'zebra-keys', secretKey: 'zebra_consul_access_token'),
                    secretEnvVar(key: 'ANSIBLE_VAULT_PASS_FILE', secretName: 'zebra-keys', secretKey: 'zebra_ansible_vault_pass'),
                    secretEnvVar(key: 'GITLAB_API_TOKEN_TEXT', secretName: 'zebra-keys', secretKey: 'zebra_gitlab_api_token'),
                    secretEnvVar(key: 'GCLOUD_ACCESS_KEY', secretName: 'zebra-keys', secretKey: 'zebra_gcloud_key'),
                    secretEnvVar(key: 'HELM_ZEBRA_SECRETS_FILE', secretName: 'zebra-keys', secretKey: 'zebra_cicd_helm_secret_values'),
                ],
            ))
        }
    }

    def creds = [string(credentialsId: 'DO_TOKEN', variable: 'DIGITALOCEAN_TOKEN')]
    withCredentials(creds) {
        podTemplate(
            label: nodeName,
            containers: containersToExecute,
        ) {
            node(nodeName) {
                controller.scmCheckout()
                controller.context.workspace = pwd()
                for (def i = 0; i < containers.size(); i++) {
                    container(containers[i].name.replaceAll('\\.','-').replaceAll('_','-')) {
                        if (controller.context.results) {
                            controller.script.echo "DrupipeExecuteKubernetes().container: serializeAndDeserialize(pipeline.context.results) BEFORE0"
                            controller.utils.serializeAndDeserialize(controller.context.results)
                            controller.script.echo "DrupipeExecuteKubernetes().container: serializeAndDeserialize(pipeline.context.results) AFTER0"
                        }
                        unstash('config')

                        for (block in containers[i].blocks) {
//                            controller.utils.debugLog(controller.context, block, 'CONTAINER BLOCK', [debugMode: 'json'], [], true)
                            sshagent([controller.context.credentialsId]) {
                                if (controller.context.results) {
                                    controller.script.echo "DrupipeExecuteKubernetes().block: serializeAndDeserialize(pipeline.context.results) BEFORE0"
                                    controller.utils.serializeAndDeserialize(controller.context.results)
                                    controller.script.echo "DrupipeExecuteKubernetes().block: serializeAndDeserialize(pipeline.context.results) AFTER0"
                                }
                                def drupipeContainerBlock = new DrupipeContainerBlock(block)
                                drupipeContainerBlock.controller = controller
                                drupipeContainerBlock.execute()

                                if (controller.context.results) {
                                    controller.script.echo "DrupipeExecuteKubernetes().block: serializeAndDeserialize(pipeline.context.results) BEFORE0"
                                    controller.utils.serializeAndDeserialize(controller.context.results)
                                    controller.script.echo "DrupipeExecuteKubernetes().block: serializeAndDeserialize(pipeline.context.results) AFTER0"
                                }
                            }
                        }
                        if (controller.context.results) {
                            controller.script.echo "DrupipeExecuteKubernetes().container: serializeAndDeserialize(pipeline.context.results) BEFORE0"
                            controller.utils.serializeAndDeserialize(controller.context.results)
                            controller.script.echo "DrupipeExecuteKubernetes().container: serializeAndDeserialize(pipeline.context.results) AFTER0"
                        }
                    }
                }
            }
        }
    }
}

