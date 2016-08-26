import groovy.json.*

def call(value, String dumpName = '') {
    if (dumpName) {
        echo dumpName
    }
    echo JsonOutput.prettyPrint(JsonOutput.toJson(value))
}

