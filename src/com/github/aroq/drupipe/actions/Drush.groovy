package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class Drush extends BaseAction {

    def context

    def script

    def utils

    def DrupipeAction action

    def runCommand() {
        def drush_dsn = (this.context.drush_dsn && this.context.drush_dsn.length() != 0) ? "${this.context.drush_dsn}" : null
        def command = (this.context.drush_command && this.context.drush_command.length() != 0) ? "${this.context.drush_command}" : 'core-status'
        def site = (this.context.drush_site && this.context.drush_site.length() != 0) ? "${this.context.drush_site}" : 'default'
        def docroot = (this.context.drush_site_docroot && this.context.drush_site_docroot.length() != 0) ? "${this.context.drush_site_docroot}" : 'docroot'
        def environment = (this.context.drush_environment && this.context.drush_environment.length() != 0) ? "${this.context.drush_environment}" : 'dev'
        def user
        def host
        def root

        if(!drush_dsn) {
            if (context.environments[environment]) {
                def env = context.environments[environment]

                if (env.root) {
                    root = env.root
                }
                else {
                    throw "DRUSH ACTION: Project root not found."
                }

                if (env.server && context.servers[env.server]) {
                    def server = context.servers[env.server]

                    if (server.user) {
                        user = server.user
                    }
                    else {
                        throw "DRUSH ACTION: Server SSH user not found."
                    }

                    if (server.host) {
                        host = server.host
                    }
                    else {
                        throw "DRUSH ACTION: Server host not found."
                    }
                }
                else {
                    throw "DRUSH ACTION: Server configuration not found."
                }
            }
            else {
                throw "DRUSH ACTION: Environment configuration not found."
            }
            drush_dsn = "${user}@${host}${root}${docroot}#${site}"
        }

        def drushString = "drush \"${drush_dsn}\" ${command} --ssh-options=\"-o StrictHostKeyChecking=no\""

        this.script.echo "Execute Drush command: ${drushString}"
        this.script.drupipeShell("${drushString}", context)
    }
}
