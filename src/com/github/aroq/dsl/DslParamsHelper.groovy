package com.github.aroq.dsl

import jenkins.model.Jenkins

class DslParamsHelper {

    def script

    def config

    ArrayList getNodeParams(job, config) {
        ArrayList result = []
        def jenkins = Jenkins.instance
        def labels = jenkins.model.Jenkins.instance.getLabels()
        if (job.value.containsKey('pipeline') && job.value.pipeline.containsKey('blocks')) {
            for (pipeline_block in job.value.pipeline.blocks) {
                def entry = [:]
                if (config.blocks.containsKey(pipeline_block)) {
                    def block_config = config.blocks[pipeline_block]
                    if (block_config.containsKey('nodeName')) {
                        entry.nodeName = block_config['nodeName']
                        println "Default nodeName for ${pipeline_block}: ${entry.node_name}"
                        entry.nodeParamName = pipeline_block.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase() + '_' + 'node_name'
                        entry.labels = labels
                        result += entry
                    }
                }
            }
        }
        result
    }

    def drupipeParamsDefault(context, job, config) {
        drupipeParameterSeparatorLevel1(context, 'GENERAL PARAMETERS')

        drupipeParameterSeparatorLevel2(context, 'Common parameters')
        drupipeParameterDebugEnabled(context)
        drupipeParameterForce(context)

        drupipeParameterSeparatorLevel2(context, 'Block parameters')

        drupipeParamNodeNameSelects(context, job, config)
        drupipeParamDisableBlocksCheckboxes(context, job)

        if (job.value.containsKey('notify')) {
            drupipeParameterSeparatorLevel2(context, 'Notification parameters')
            drupipeParamMuteNotificationCheckboxes(context, job)
        }

        if (job.value.containsKey('trigger')) {
            drupipeParameterSeparatorLevel2(context, 'Trigger parameters')
            drupipeParamDisableTriggersCheckboxes(context, job)
            drupipeParamTriggerParams(context, job)
        }
    }

    def drupipeParameterSeparatorLevel1(context, header, color = 'green', bold = true, height = '4px', fontSize = '16px') {
        drupipeParameterSeparatorStylized(context, header, color, bold, height, fontSize)
    }

    def drupipeParameterSeparatorLevel2(context, header, color = 'green', bold = true, height = '2px', fontSize = '14px') {
        drupipeParameterSeparatorStylized(context, header, color, bold, height, fontSize)
    }

    def drupipeParameterSeparatorStylized(context, header, color, bold = false, height = '2px', fontSize = '14px') {
        bold = bold ? ' font-weight: bold;' : ''
        drupipeParameterSeparator(
            context,
            'separator',
            header,
            "margin-top:10px; margin-bottom:10px; color: ${color}; background-color: ${color}; border: 0 none; height: ${height}",
            "font-size: ${fontSize}; color: ${color};${bold}"
        )
    }

    def drupipeParameterSeparator(context, separatorName, header, style = '', headerStyle = '') {
        context.parameterSeparatorDefinition {
            name(separatorName)
            separatorStyle(style)
            sectionHeader(header)
            sectionHeaderStyle(headerStyle)
        }
    }

    def drupipeParameterDebugEnabled(context) {
        context.stringParam('debugEnabled', '0')
    }

    def drupipeParameterForce(context) {
        context.stringParam('force', '0')
    }

    def drupipeParamChoices(context, paramName, paramDescription, paramType, paramScript, sandboxMode = true, paramFilterable = false, paramFilterLength = 0) {
        context.choiceParameter() {
            name(paramName)
            choiceType(paramType)
            description(paramDescription)
            script {
                groovyScript {
                    script {
                        sandbox(sandboxMode)
                        script(paramScript)
                    }
                    fallbackScript {
                        script('')
                        sandbox(sandboxMode)
                    }
                }
            }
            randomName(paramName)
            filterable(paramFilterable)
            filterLength(paramFilterLength)
        }
    }

    def drupipeParamTagsSelectsRelease(context, job, config, name, project) {
        println "Project: ${project.value.name}"
        def projectRepo = project.value.repo
        println "Repo: ${projectRepo}"
        drupipeParamChoices(
            context,
            name,
            'Allows to select tag',
            'PT_SINGLE_SELECT',
            activeChoiceGetTagsChoicesScript(projectRepo, '*', 'x.y.z'),
            false,
            true
        )
    }

    def drupipeParamTagsSelectsDeploy(context, job, config, name, project) {
        println "Project: ${project.value.name}"
        def releaseRepo
        if (job.value.containsKey('source') && job.value.source.containsKey('version_source')) {
            releaseRepo = job.value.source.version_source
        }
        else {
            releaseRepo = project.value.type == 'root' ? project.value.repo : project.value.root_repo
        }
        println "Repo: ${releaseRepo}"
        if (releaseRepo) {
            drupipeParamChoices(
                context,
                name,
                'Allows to select tag',
                'PT_SINGLE_SELECT',
                activeChoiceGetTagsChoicesScript(releaseRepo, '*', ''),
                false,
                true
            )
        }
    }

    def drupipeParamBranchesSelectsDeploy(context, job, config, name, project) {
        println "Project: ${project.value.name}"
        def releaseRepo
        if (job.value.containsKey('source') && job.value.source.containsKey('version_source')) {
            releaseRepo = job.value.source.version_source
        }
        else {
            releaseRepo = project.value.type == 'root' ? project.value.repo : project.value.root_repo
        }
        println "Repo: ${releaseRepo}"
        if (releaseRepo) {
            drupipeParamChoices(
                context,
                name,
                'Allows to select branch',
                'PT_SINGLE_SELECT',
                activeChoiceGetBranchesChoicesScript(releaseRepo, job.value.source.pattern),
                false,
                true
            )
        }
    }

    def drupipeParamOperationsCheckboxes(context, job, config) {
        if (config.operationsModes) {
            drupipeParamChoices(
                context,
                'operationsMode',
                'Allows to select operations mode',
                'PT_SINGLE_SELECT',
                activeChoiceGetChoicesScript(config.operationsModes, ''),
            )
        }
    }

    def drupipeParamNodeNameSelects(context, job, config) {
        for (nodeParam in getNodeParams(job, config)) {
            drupipeParamChoices(
                context,
                nodeParam.nodeParamName,
                'Allows to select node to run pipeline block',
                'PT_SINGLE_SELECT',
                activeChoiceGetChoicesScript(nodeParam.labels.collect { it.toString() }, nodeParam.nodeName)
            )
        }
    }

    def drupipeParamDisableBlocksCheckboxes(context, job) {
        if (job.value.containsKey('pipeline') && job.value.pipeline.containsKey('blocks')) {
            drupipeParamChoices(
                context,
                'disable_block',
                'Allows to disable pipeline blocks',
                'PT_CHECKBOX',
                activeChoiceGetChoicesScript(job.value.pipeline.blocks.collect { it }, ''),
            )
        }
    }

    def drupipeParamMuteNotificationCheckboxes(context, job) {
        if (job.value.containsKey('notify')) {
            drupipeParamChoices(
                context,
                'mute_notification',
                'Allows to mute notifications in selected channels',
                'PT_CHECKBOX',
                activeChoiceGetChoicesScript(job.value.notify.collect { it }, ''),
            )
        }
    }

    def drupipeParamDisableTriggersCheckboxes(context, job) {
        if (job.value.containsKey('trigger')) {
            drupipeParamChoices(
                context,
                'disable_trigger',
                'Allows to disable post build job trigger',
                'PT_CHECKBOX',
                activeChoiceGetChoicesScript(job.value.trigger.collect { it.name }, ''),
            )
        }
    }

    def drupipeParamTriggerParams(context, job) {
        if (job.value.containsKey('trigger')) {
            for (trigger_job in job.value.trigger) {
                if (trigger_job.containsKey('params')) {
                    for (param in trigger_job.params) {
                        def trigger_job_name_safe = trigger_job.name.replaceAll(/^[^a-zA-Z_$]+/, '').replaceAll(/[^a-zA-Z0-9_]+/, "_").toLowerCase()
                        context.stringParam(trigger_job_name_safe + '_' + param.key, param.value)
                    }
                }
            }
        }
    }

    def activeChoiceGetChoicesScript(ArrayList choices, String defaultChoice) {
        String choicesString = choices.join('|')
        def script =
            """
def choices = "${choicesString}"
def defaultChoice = "${defaultChoice}"
choices = choices.tokenize('|')
defaultChoice = defaultChoice.tokenize('|')

for (def i = 0; i < choices.size(); i++) {
  if (choices[i] in defaultChoice) {
    choices[i] = choices[i] + ':selected'
  }
}

choices

"""
        script
    }

    def activeChoiceGetTagsChoicesScript(String url, String tagPattern, String sort) {
        def script =
            """
import jenkins.model.*
import hudson.model.*
import hudson.EnvVars
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId
import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull

import org.jenkinsci.plugins.gitclient.Git
import org.jenkinsci.plugins.gitclient.GitClient
import hudson.plugins.git.*

import java.util.regex.Pattern

/**
 * version number model: prefix-X.Y.Z
 */
class Version {

  static def pattern3 = Pattern.compile("(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)")
    static def pattern4 = Pattern.compile("(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)")
    static def pattern5 = Pattern.compile("(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)")
    static def pattern6 = Pattern.compile("(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)\\\\.(\\\\d+)")

  /** parse version from text */
  static def from(text){
    def matcher3 = pattern3.matcher(text);
        def matcher4 = pattern4.matcher(text);
        def matcher5 = pattern5.matcher(text);
        def matcher6 = pattern6.matcher(text);
    if (matcher6.find()) {
      new Version( major:matcher6.group(1), minor:matcher6.group(2), patch:matcher6.group(3), patch2:matcher6.group(4), patch3:matcher6.group(5), patch4:matcher6.group(6)  )
    }
        else if(matcher5.find()) {
      new Version( major:matcher5.group(1), minor:matcher5.group(2), patch:matcher5.group(3), patch2:matcher5.group(4), patch3:matcher5.group(5)  )
    }
        else if(matcher4.find()) {
      new Version( major:matcher4.group(1), minor:matcher4.group(2), patch:matcher4.group(3), patch2:matcher4.group(4)  )
    }
        else if(matcher3.find()) {
      new Version( major:matcher3.group(1), minor:matcher3.group(2), patch:matcher3.group(3)  )
    }
        else {
      new Version( major:"0", minor:"0", patch:"0" )
    }
  }

  String prefix, major, minor, patch, patch2, patch3, patch4

  /** padded form for alpha sort */
  def String toString() {
        if (patch4) {
            String.format('%010d-%010d-%010d-%010d-%010d-%010d', major.toInteger(), minor.toInteger(), patch.toInteger(), patch2.toInteger(), patch3.toInteger(), patch4.toInteger())
        }
        else if (patch3) {
            String.format('%010d-%010d-%010d-%010d-%010d', major.toInteger(), minor.toInteger(), patch.toInteger(), patch2.toInteger(), patch3.toInteger())
        }
        else if (patch2) {
            String.format('%010d-%010d-%010d-%010d', major.toInteger(), minor.toInteger(), patch.toInteger(), patch2.toInteger())
        }
        else {
            String.format('%010d-%010d-%010d', major.toInteger(), minor.toInteger(), patch.toInteger())
        }


  }

}

/**
 * Util method to find credential by id in jenkins
 *
 * @param credentialsId credentials to find in jenkins
 *
 * @return {@link CertificateCredentials} or {@link StandardUsernamePasswordCredentials} expected
 */
def Credentials lookupSystemCredentials(String credentialsId) {
    return firstOrNull(
        com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            Credentials.class,
            Jenkins.getInstance(),
            null,
            Collections.<DomainRequirement>emptyList()
        ),
        withId(credentialsId)
    );
}

def getTags(GitClient gitClient, String gitUrl, tagPattern) {
    tagSet = []
    try {
        def tags = gitClient.getRemoteReferences(gitUrl, tagPattern, false, true);
        for (String tagName : tags.keySet()) {
            tagSet << tagName.replaceFirst(".*refs/tags/", "");
        }
    } catch (GitException e) {
        tagSet = 'failed'
    }
    return tagSet.sort().reverse();
}

Credentials credentials = lookupSystemCredentials('zebra')

// get git executable on master
EnvVars environment;
final Jenkins jenkins = Jenkins.getActiveInstance();
environment = jenkins.toComputer().buildEnvironment(TaskListener.NULL);

GitClient git = Git.with(TaskListener.NULL, environment)
    .using(GitTool.getDefaultInstallation().getGitExe())
    .getClient();
git.addDefaultCredentials(credentials);

/**
 * @return sorted tag list as script result
 */
try {

  def tagList = getTags(git, '${url}', '${tagPattern}')

    if ('${sort}' == 'x.y.z') {
      if (tagList) {
    tagList.sort{ tag -> Version.from(tag).toString() }.reverse()
      } else {
          [ 'master' ] // no tags in git repo
      }
    }
  else {
    tagList
  }

} catch( e )  {

  [ e.toString() ]

}

"""
        script
    }

    def activeChoiceGetBranchesChoicesScript(String url, String branchesPattern) {
        def script =
            """
import jenkins.model.*
import hudson.model.*
import hudson.EnvVars
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId
import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull

import org.jenkinsci.plugins.gitclient.Git
import org.jenkinsci.plugins.gitclient.GitClient
import hudson.plugins.git.*

/**
 * Util method to find credential by id in jenkins
 *
 * @param credentialsId credentials to find in jenkins
 *
 * @return {@link CertificateCredentials} or {@link StandardUsernamePasswordCredentials} expected
 */
def Credentials lookupSystemCredentials(String credentialsId) {
    return firstOrNull(
        com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            Credentials.class,
            Jenkins.getInstance(),
            null,
            Collections.<DomainRequirement>emptyList()
        ),
        withId(credentialsId)
    );
}

def getTags(GitClient gitClient, String gitUrl, tagPattern) {
    tagSet = []
    try {
        def tags = gitClient.getRemoteReferences(gitUrl, tagPattern, true, false);
        for (String tagName : tags.keySet()) {
            tagSet << tagName.replaceFirst(".*refs/heads/", "");
        }
    } catch (GitException e) {
        tagSet = 'failed'
    }
    return tagSet.sort().reverse();
}

Credentials credentials = lookupSystemCredentials('zebra')

// get git executable on master
EnvVars environment;
final Jenkins jenkins = Jenkins.getActiveInstance();
environment = jenkins.toComputer().buildEnvironment(TaskListener.NULL);

GitClient git = Git.with(TaskListener.NULL, environment)
    .using(GitTool.getDefaultInstallation().getGitExe())
    .getClient();
git.addDefaultCredentials(credentials);

return getTags(git, '${url}', '${branchesPattern}')

"""
        script
    }
}
