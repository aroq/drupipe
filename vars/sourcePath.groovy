def call(params, sourceName, String path) {
    debugLog(params, sourceName, 'Source name')
    if (sourceName in params.sources) {
        params.sources[sourceName].path + '/' + path
    }
}
