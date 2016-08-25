def setFoo(v) {
    this.f = v;
    echo '---------------------'
    echo '---------------------'
    echo '---------------------'
    echo '---------------------'
    echo '---------------------'
    echo '---------------------'
    echo '---------------------'
    echo '---------------------'
    echo '---------------------'
    echo '---------------------'
    dump this.f
}
def getFoo() {
    return this.binding['f'];
}
def say(name) {
    echo "Hello world, ${name}"
}