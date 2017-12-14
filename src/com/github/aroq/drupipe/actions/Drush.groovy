package com.github.aroq.drupipe.actions

class Drush extends BaseAction {

    def runCommand() {
        def drush_dsn = (action.pipeline.context.env.drush_dsn && action.pipeline.context.env.drush_dsn.length() != 0) ? "${action.pipeline.context.env.drush_dsn}" : null
        def command = (action.pipeline.context.env.drush_command && action.pipeline.context.env.drush_command.length() != 0) ? "${action.pipeline.context.env.drush_command}" : 'core-status'
        def site = (action.pipeline.context.env.drush_site && action.pipeline.context.env.drush_site.length() != 0) ? "${action.pipeline.context.env.drush_site}" : 'default'
        def docroot = (action.pipeline.context.env.drush_site_docroot && action.pipeline.context.env.drush_site_docroot.length() != 0) ? "${action.pipeline.context.env.drush_site_docroot}" : 'docroot'
        def environment = (action.pipeline.context.env.drush_environment && action.pipeline.context.env.drush_environment.length() != 0) ? "${action.pipeline.context.env.drush_environment}" : 'dev'
        def user
        def host
        def root

        if (!drush_dsn) {
            if (action.pipeline.context.environments[environment]) {
                def env = action.pipeline.context.environments[environment]

                if (env.root) {
                    root = env.root
                }
                else {
                    throw new Exception("DRUSH ACTION: Project root not found.")
                }

                if (env.server && action.pipeline.context.servers[env.server]) {
                    def server = action.pipeline.context.servers[env.server]

                    if (server.user) {
                        user = server.user
                    }
                    else {
                        throw new Exception("DRUSH ACTION: Server SSH user not found.")
                    }

                    if (server.host) {
                        host = server.host
                    }
                    else {
                        throw new Exception("DRUSH ACTION: Server host not found.")
                    }
                }
                else {
                    throw new Exception("DRUSH ACTION: Server configuration not found.")
                }
            }
            else {
                throw new Exception("DRUSH ACTION: Environment configuration not found.")
            }
            drush_dsn = "${user}@${host}${root}${docroot}#${site}"
        }

        def drushString = "drush \"${drush_dsn}\" ${command} --ssh-options=\"-o StrictHostKeyChecking=no\""

        this.script.echo "Execute Drush command: ${drushString}"

        if (action.params.store_result || action.pipeline.context.job.notify) {
            this.script.echo "Return Drush output."
            def result = this.script.drupipeShell("${drushString}", [return_stdout: true])
            result.stdout = result.stdout.replaceAll(/(?m)^(PHP )?Deprecated.*$/, '').trim()
            result.result = drushString
            return result
        }
        else {
            this.script.drupipeShell("${drushString}", action.params)
        }
    }
}
