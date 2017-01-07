package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {
    ArrayList<Stage> stages = []

    LinkedHashMap params = [:]
}
