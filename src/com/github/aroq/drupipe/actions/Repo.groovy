package com.github.aroq.drupipe.actions

@Grab('org.yaml:snakeyaml:1.17')

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor

import com.github.aroq.StateStableInfo
import com.github.aroq.BuildFileInfo

class Repo extends BaseAction {

    def init() {
        context << script.drupipeAction([action: "Docman.init"], context)
        context
    }

    // Build project from repo & execute commands from .build.yaml.
    def build() {
        init()
        script.echo "PROJECT NAME: ${context.projectName}"
        if (context.projectName) {
            if (!context['builder']) {
                context['builder'] = [:]
            }
            context.builder['repoUrl'] = context.env.gitlabSourceRepoURL ? context.env.gitlabSourceRepoURL : ''
            context.builder['buildDir'] = context.projectName
            context.builder['buildName'] = context.projectName

            def repoVersionBranch = context.env.gitlabSourceBranch ? context.env.gitlabSourceBranch : 'state_stable'
            StateStableInfo stateStableInfo = getStableInfo(context.builder['repoUrl'], repoVersionBranch)

            context.builder['version'] = stateStableInfo.version
            context.builder['buildName'] = context.projectName

            // Checkout stable version.
            script.drupipeShell(
                """
            rm -fR ${context.builder['buildDir']}
            git clone --depth 1 -b ${context.builder['version']} ${context.builder['repoUrl']} ${context.builder['buildDir']}
            """, action.params
            )

            def buildScript = null

            // Check build script exists.
            if (script.fileExists("${context.builder['buildDir']}/.build.yaml")) {
                buildScript = "${context.builder['buildDir']}/.build.yaml"
            }
            else if (script.fileExists("${context.builder['buildDir']}/.build.yml")) {
                buildScript = "${context.builder['buildDir']}/.build.yml"
            }

            // Execute commands from .build.yaml or .build.yml.
            if (buildScript != null) {
                BuildFileInfo getYamlCommands = getBuildFileInfo(readFile(buildScript))
                for (cmd in getYamlCommands.build) {
                    drupipeShell(
                        """
                        cd ${context.builder['buildDir']}
                        ${cmd}
                        """, action.params
                    )
                }
            }
        }
        context
    }

    def getStableInfo(repoUrl, repoVersionBranch) {
        // Get stable version from repo.
        script.dir("${context.projectName}") {
            git credentialsId: context.credentialsId, url: repoUrl, branch: repoVersionBranch
        }
        StateStableInfo stateStableInfo = getStableTag(script.readFile("${context.projectName}/info.yaml"))
        script.echo "VERSION: ${stateStableInfo.version}"
        stateStableInfo
    }

    @NonCPS
    def getStableTag(yamlFile) {
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()));
        StateStableInfo stableStableInfo = yaml.loadAs(yamlFile, StateStableInfo.class);
        return stableStableInfo
    }

    def getBuildFileInfo(yamlFile) {
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()));
        BuildFileInfo buildFileInfo = yaml.loadAs(yamlFile, BuildFileInfo.class);
        return buildFileInfo
    }
}
