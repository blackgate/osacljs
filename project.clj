(defproject osacljs "0.1.0-SNAPSHOT"
  :description "Use Replumb in OSX Javascript for Automation"
  :url "https://github.com/blackgate/osacljs"
  :license {:name "Eclipse Public License"
            :url "https://github.com/blackgate/osacljs/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [replumb/replumb "0.2.4"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [malabarba/lazy-map "1.3"]]
  :clean-targets [:target-path "out"]
  :plugins [[lein-cljsbuild "1.1.5"]]
  :cljsbuild {
    :builds [{
        :source-paths ["src"]
        :compiler {
          :main          osacljs.core
          :output-to     "out/main.js"
          :output-dir    "out"
          :optimizations :whitespace}}]})
