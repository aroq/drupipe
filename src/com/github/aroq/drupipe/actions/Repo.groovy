package com.github.aroq.drupipe.actions

@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor

import com.github.aroq.StateStableInfo
import com.github.aroq.BuildFileInfo

def init(params) {
    params << drupipeAction([action: "Docman.init"], params)
    params << [returnConfig: true]
}

// Build project from repo & execute commands from .build.yaml.
def build(params) {
    init(params)
    echo "PROJECT NAME: ${params.projectName}"
    if (params.projectName) {
        if (!params['builder']) {
            params['builder'] = [:]
        }
        params.builder['repoUrl'] = env.gitlabSourceRepoURL ? env.gitlabSourceRepoURL : ''
        params.builder['buildDir'] = params.projectName
        params.builder['buildName'] = params.projectName

        def repoVersionBranch = env.gitlabSourceBranch ? env.gitlabSourceBranch : 'state_stable'
        stateStableInfo = getStableInfo(params, params.builder['repoUrl'], repoVersionBranch)

        params.builder['version'] = stateStableInfo.version
        params.builder['buildName'] = params.projectName

        // Checkout stable version.
        drupipeShell(
            """
            rm -fR ${params.builder['buildDir']}
            git clone --depth 1 -b ${params.builder['version']} ${params.builder['repoUrl']} ${params.builder['buildDir']}
            """, params << [shellCommandWithBashLogin: true]
        )

        // Execute commands from .build.yaml.
        if (fileExists("${params.builder['buildDir']}/.build.yml")) {
            BuildFileInfo getYamlCommands = getBuildFileInfo(readFile("${params.builder['buildDir']}/.build.yml"))
            for (cmd in getYamlCommands.build) {
                drupipeShell(
                    """
                    cd ${params.builder['buildDir']}
                    ${cmd}
                    """, params << [shellCommandWithBashLogin: true]
                )
            }
        }
    }
    params << [returnConfig: true]
}

def getStableInfo(params, repoUrl, repoVersionBranch) {
    // Get stable version from repo.
    dir("${params.projectName}") {
        git credentialsId: params.credentialsID, url: repoUrl, branch: repoVersionBranch
    }
    StateStableInfo stateStableInfo = getStableTag(readFile("${params.projectName}/info.yaml"))
    echo "VERSION: ${stateStableInfo.version}"
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
