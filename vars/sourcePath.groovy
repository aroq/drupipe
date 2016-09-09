def call(params, sourceName, String path) {
    debug(params, sourceName, 'Source name')
    if (sourceName in params.sources) {
        params.sources[sourceName].path + '/' + path
    }
}
