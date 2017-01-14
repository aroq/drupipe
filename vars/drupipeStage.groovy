import com.github.aroq.drupipe.DrupipeStage

def call(name, context, body) {
    new DrupipeStage(name: name, params: context).execute(context, body)
}

