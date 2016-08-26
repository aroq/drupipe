package com.github.aroq.workflowlibs

class Action implements Serializable {
    String name
    String methodName
    HashMap params = [:]
}