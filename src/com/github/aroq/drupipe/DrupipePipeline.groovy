package com.github.aroq.drupipe

class DrupipePipeline implements Serializable {
    List<Stage> stages
    HashMap params = [:]
}
