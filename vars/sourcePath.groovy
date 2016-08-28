def call(params, sourceName, String path) {
    echo "Source name: ${sourceName}"
    dump(params, 'sourcePath')
    if (sourceName in params.sources) {
        params.sources[sourceName].path + '/' + path
    }
}
