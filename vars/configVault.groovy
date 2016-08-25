def set(config) {
   this.config = config
}

def get() {
   this.config
}

def get(name) {
   if (this.config[name]) {
      this.config[name]
   }
   else {
      null
   }
}
