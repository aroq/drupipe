package com.github.aroq.drupipe

class DrupipeJob extends DrupipeBase {

    String name

    String type

    String from

    String branch

    String state

    String env

    String configRepo

    HashMap source

    def triggers

    def webhooks

    def context

    DrupipePipeline pipeline

    DrupipeController controller

    def execute(body = null) {
        controller.script.echo "DrupipeJob execute - ${name}"
        pipeline.controller = controller
        pipeline.execute()
    }

}
