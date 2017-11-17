package com.github.aroq.drupipe.processors

interface DrupipeProcessor {
    def process(context, obj, parent, key, mode)
}

