import com.github.aroq.drupipe.DrupipeController
import com.github.aroq.drupipe.DrupipePod
import org.csanchez.jenkins.plugins.kubernetes.model.SecretEnvVar;
import io.fabric8.kubernetes.api.model.EnvVar;


def call(DrupipePod pod, ArrayList unstash = [], ArrayList stash = [], unipipe_retrieve_config = false) {
    DrupipeController controller = pod.controller
    controller.drupipeLogger.debug "Container mode: kubernetes"
    controller.drupipeLogger.debug "Pod name: ${pod.name}"
    controller.drupipeLogger.debug "Pod idleMinutes: ${pod.idleMinutes}"

    String podTitle = "POD, Idle minutes: ${pod.idleMinutes}"

    if (pod.name) {
        podTitle = "POD: ${pod.name}, Idle minutes: ${pod.idleMinutes}"
    }

    controller.utils.echoMessage '[COLLAPSED-END]'
    controller.utils.echoMessage "[COLLAPSED-START] ${podTitle}"

    def nodeName = pod.name
    if (pod.name == null) {
        // SHA1 hash of job BUILD_TAG to make pod name unique.
        def sha = controller.utils.getSHA1(controller.context.env.BUILD_TAG)
        nodeName = "${controller.context.env.BUILD_TAG.take(45).replaceAll(/^[^a-zA-Z0-9]/, "").replaceAll(/[^a-zA-Z0-9]$/, "")}-${sha.take(8).replaceAll(/^[^a-zA-Z0-9]/, "").replaceAll(/[^a-zA-Z0-9]$/, "")}"
    }
    def containerNames = []
    def containersToExecute= []


    def env_vars
    env_vars << new EnvVar('TF_VAR_consul_address', controller.context.env.TF_VAR_consul_address)
    env_vars <<  new EnvVar('UNIPIPE_SOURCES', controller.context.env.UNIPIPE_SOURCES)
    env_vars <<  new SecretEnvVar(key: 'DIGITALOCEAN_TOKEN', secretName: 'zebra-keys', secretKey: 'zebra_do_token')
    env_vars <<  new SecretEnvVar(key: 'ANSIBLE_VAULT_PASS_FILE', secretName: 'zebra-keys', secretKey: 'zebra_ansible_vault_pass')
    env_vars <<  new SecretEnvVar(key: 'GITLAB_API_TOKEN_TEXT', secretName: 'zebra-keys', secretKey: 'zebra_gitlab_api_token')
    env_vars << new SecretEnvVar(key: 'GCLOUD_ACCESS_KEY', secretName: 'zebra-keys', secretKey: 'zebra_gcloud_key')
    env_vars << new SecretEnvVar(key: 'HELM_ZEBRA_SECRETS_FILE', secretName: 'zebra-keys', secretKey: 'zebra_cicd_helm_secret_values')
    for (def i = 0; i < 3; i++) {
        env_vars << new SecretEnvVar(key: "DIGITALOCEAN_TOKEN-${i}", secretName: 'zebra-keys', secretKey: 'zebra_do_token')
    }

    for (def i = 0; i < pod.containers.size(); i++) {
        def container = pod.containers[i]
        def containerName = container.name.replaceAll('\\.','-').replaceAll('_','-').take(30).replaceAll(/^[^a-zA-Z0-9]/, "").replaceAll(/[^a-zA-Z0-9]$/, "")
        if (!containerNames.contains(containerName)) {
            controller.drupipeLogger.debug "Create k8s containerTemplate for container: ${container.name}, image: ${container.image}"
            containerNames += containerName
            containersToExecute.add(containerTemplate(
                name:                  containerName,
                image:                 container.image,
                ttyEnabled:            container.k8s.ttyEnabled,
                command:               container.k8s.command,
                resourceRequestCpu:    container.k8s.resourceRequestCpu,
                resourceLimitCpu:      container.k8s.resourceLimitCpu,
                resourceRequestMemory: container.k8s.resourceRequestMemory,
                resourceLimitMemory:   container.k8s.resourceLimitMemory,
                alwaysPullImage:       container.k8s.alwaysPullImage,
                // TODO: move in configs.
                envVars:  env_vars,
            ))
        }
    }

    podTemplate(
        label: nodeName,
        containers: containersToExecute,
        idleMinutes: pod.idleMinutes,
    ) {
        node(nodeName) {
            if (unipipe_retrieve_config) {
                controller.utils.getUnipipeConfig(controller)
            }
            else {
                controller.drupipeLogger.warning "Retrieve config disabled in config."
            }
            controller.utils.unstashList(controller, unstash)
            controller.context.workspace = pwd()
            for (def i = 0; i < pod.containers.size(); i++) {
                container(pod.containers[i].name.replaceAll('\\.','-').replaceAll('_','-').take(30).replaceAll(/^[^a-zA-Z0-9]/, "").replaceAll(/[^a-zA-Z0-9]$/, "")) {
                    sshagent([controller.context.credentialsId]) {
                        // To have k8s envVars & secretEnvVars as well.
                        controller.context.env = controller.utils.merge(controller.context.env, controller.utils.envToMap())
//                        controller.utils.echoMessage '[COLLAPSED-END]'
                        pod.containers[i].execute()
//                        controller.utils.echoMessage '[COLLAPSED-START] ...'
                    }
                }
            }
            controller.utils.stashList(controller, stash)
            controller.utils.echoMessage '[COLLAPSED-END]'
        }
    }

}
