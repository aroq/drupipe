package com.github.aroq.drupipe.actions

import com.github.aroq.drupipe.DrupipeActionConroller

class BaseAction implements Serializable {

    def context

    def script

    def utils

    def DrupipeActionConroller action

}