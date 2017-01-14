import com.github.aroq.drupipe.DrupipePipeline

def call(context = [:], body) {
    context << params
    context.pipeline = new DrupipePipeline(context: context, script: this)
    context.pipeline.execute(body)
}
