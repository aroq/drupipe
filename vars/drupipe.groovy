import com.github.aroq.drupipe.DrupipePipeline

def call(context = [:], body) {
    context.pipeline = new DrupipePipeline(context: context, script: this, params: params)
    context.pipeline.execute(body)
}
