def call(params, sourceName, String path) {
    debug "Source name: ${sourceName}"
    if (sourceName in params.sources) {
        params.sources[sourceName].path + '/' + path
    }
}
