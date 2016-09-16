def call(paramName) {
    try {
        result = "${paramName}"
        if (result != paramName) {
            result
        }
        else {
            false
        }
    }
    catch (err) {
        false
    }
}

