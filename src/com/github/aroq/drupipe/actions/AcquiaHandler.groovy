package com.github.aroq.drupipe.actions

class AcquiaHandler extends BaseAction {

    def deploy() {
        action.pipeline.executeAction(
            action: 'Druflow.deploy',
            params: this.action.params,
        )
    }

}
