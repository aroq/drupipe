package com.github.aroq.drupipe.actions

def set(params) {
    // TODO: do it with default parameters map.
    drupipeShell("git config --global user.email 'drupipe@github.com'; git config --global user.name 'Drupipe'", params)
}
