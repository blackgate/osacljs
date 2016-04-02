(defproject osacljs "0.1.0-SNAPSHOT"
  :description "Use Replumb in OSX Javascript for Automation"
  :url "https://github.com/blackgate/osacljs"
  :license {:name "Eclipse Public License"
            :url "https://github.com/blackgate/osacljs/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [replumb/replumb "0.2.1"]]
  :clean-targets [:target-path "out"]
  :plugins [[lein-cljsbuild "1.1.3"]]
  :cljsbuild {
    :builds [{
        :source-paths ["src"]
        :compiler {
          :main          osacljs.core
          :output-to     "out/main.js"
          :output-dir    "out"
          :optimizations :whitespace}}]})
