import com.github.aroq.drupipe.Stage

def call(name, config, body) {
    new Stage(name: name, params: config, script: this).execute(config, body)
}

