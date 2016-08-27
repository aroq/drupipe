def call(params, sourceName, String path) {
    if (sourceName in params.sources) {
        params.sources[sourceName]['path'] + '/' + path
    }
}
