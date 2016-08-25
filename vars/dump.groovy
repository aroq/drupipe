def call(params, $name = nil) {
    if ($name) {
        echo "Dumping ${name} values:"
    }
    for (item in params) {
        echo "${item.key} = ${item.value}"
    }
}