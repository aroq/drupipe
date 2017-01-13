import com.github.aroq.drupipe.DrupipeStage

def call(name, config, body) {
    new DrupipeStage(name: name, params: config).execute(config, body)
}

