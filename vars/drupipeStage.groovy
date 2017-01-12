import com.github.aroq.drupipe.Stage

def call(name, config, body) {
    echo "NEW STATE CREATE script: ${this}"
    //new Stage(name: name, params: config, script: this).execute(body)
}

