package com.github.aroq.drupipe

class Action implements Serializable {
    String action
    String name
    String methodName
    HashMap params = [:]

    String getFullName() {
        "${this.name}.${this.methodName}"
    }
}
