import com.github.aroq.drupipe.Stage

def call(name, config, body) {
    echo "drupipeStage:"
    echo "NAME: ${name}"
    echo "CONFIG: ${config}"
    new Stage(name: name, params: config, script: this).execute(body)
}

