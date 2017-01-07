package com.github.aroq.drupipe

class Stage implements Serializable {
    String name

    ArrayList<Action> actionList

    HashMap params = [:]
}
