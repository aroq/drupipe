package com.github.aroq.drupipe

class DrupipeBase implements Serializable {

    public String name

    public boolean from_processed

    public String from_processed_mode

    public String from_source

    public ArrayList fromPaths

    public DrupipeController controller

    def executeWithCollapsed(message, body) {
        controller.drupipeLogger.collapsedStart(message)
        body()
        controller.drupipeLogger.collapsedEnd()
    }

}
