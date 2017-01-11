import com.github.aroq.drupipe.Stage

def call(name, config, body) {
//    utils = new com.github.aroq.drupipe.Utils()
    new Stage(name: name, params: config, script: this).execute(body)
//    utils._executeStage(name, config, body)
}

