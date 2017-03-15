package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeAction

class BaseAction implements Serializable {

    def context

    def script

    def utils

    def DrupipeAction action

    def dumpParams() {
        utils.jsonDump(action.params, 'ACTION PARAMS')
    }

}
