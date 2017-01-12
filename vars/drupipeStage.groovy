import com.github.aroq.drupipe.Stage

def call(name, config, body) {
    new Stage(name: name, params: config).execute(config, body)
}

