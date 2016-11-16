def call(String message) {
    if (message) {
        if (message.size() < 80) {
           echo message + '-' * (80 - message.size())
        }
        else {
           echo message
        }
    }
}

