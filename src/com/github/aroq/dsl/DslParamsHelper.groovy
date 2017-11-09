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


}
