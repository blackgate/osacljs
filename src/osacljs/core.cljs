(ns osacljs.core
  (:require [clojure.string :as string]
            [replumb.core :as replumb]
            [replumb.repl :as replumb-repl]))

(def objc-import (js* "ObjC['import']"))

(objc-import "Foundation")
(objc-import "readline")

(js* "global = this")


(def fm js/$.NSFileManager.defaultManager)

(defn- multiline?
  [input]
  (try
    (replumb-repl/read-string {:features #{:cljs}} input)
    false
    (catch :default e
      (= "EOF" (subs (.-message e) 0 3)))))

(defn- prompt-read
  [prompt]
  (loop [p prompt acc ""]
    (let [input (js/$.readline p)
          form (str acc "\n" input)]
      (js/$.add_history input)
      (if (multiline? form)
        (recur (str (apply str (repeat (- (count prompt) 5) " ")) "#_=> ") form)
        form))))

(defn- enable-util-print! []
  (set! *print-newline* false)
  (letfn [(print-fn [& args]
            (.apply (.-log js/console) js/console (into-array args)))]
    (set! *print-fn* print-fn)
    (set! *print-err-fn* print-fn))
  nil)

(enable-util-print!)


;; OSA file reading fns

(defn osa-read-file-sync
  "Accepts a filename to read. Upon success, returns the source.
  Otherwise returns nil."
  [filename]
  (let [data (.contentsAtPath fm filename)]
    (js/ObjC.unwrap (js/$.NSString.alloc.initWithDataEncoding data js/$.NSUTF8StringEncoding))))


(defn osa-read-file
  "Accepts a filename to read and a callback. Upon success, invokes
  callback with the source. Otherwise invokes the callback with nil."
  [filename cb]
  (cb (osa-read-file-sync filename)))

;; Facilities for loading Closure deps

(def goog-path-root "out/goog/")

(defn closure-index
  []
  (let [paths-to-provides
        (map (fn [[_ path provides]]
               [path (map second
                       (re-seq #"'(.*?)'" provides))])
          (re-seq #"\ngoog\.addDependency\('(.*)', \[(.*?)\].*"
            (osa-read-file-sync (str goog-path-root "deps.js"))))]
    (into {}
      (for [[path provides] paths-to-provides
            provide provides]
        [(symbol provide) (str goog-path-root (second (re-find #"(.*)\.js$" path)))]))))

(def closure-index-mem (memoize closure-index))

(defn load-goog
  [name cb]
  (if-let [goog-path (get (closure-index-mem) name)]
    (if-let [source (osa-read-file-sync (str goog-path ".js"))]
      (cb {:source source
           :lang   :js})
      (cb nil))
    (cb nil)))

;; Facilities for loading files

(defn- filename->lang
  "Converts a filename to a lang keyword by inspecting the file
  extension."
  [filename]
  (if (string/ends-with? filename ".js")
    :js
    :clj))

(defn- read-some
  "Reads the first filename in a sequence of supplied filenames,
  using a supplied read-file-fn, calling back upon first successful
  read, otherwise calling back with nil."
  [[filename & more-filenames] read-file-fn cb]
  (if filename
    (read-file-fn
      filename
      (fn [source]
        (if source
          (cb {:lang   (filename->lang filename)
               :source source})
          (read-some more-filenames read-file-fn cb))))
    (cb nil)))

(defn- filenames-to-try
  "Produces a sequence of filenames to try reading, in the
  order they should be tried."
  [src-paths macros path]
  (let [extensions (if macros
                     [".clj" ".cljc"]
                     [".cljs" ".cljc" ".js"])]
    (for [extension extensions
          src-path src-paths]
      (str src-path "/" path extension))))

(defn- skip-load?
  [name macros?]
  ((if macros?
     '#{cljs.core
        cljs.js
        cljs.pprint
        cljs.repl
        cljs.env.macros
        cljs.analyzer.macros
        cljs.compiler.macros
        cljs.tools.reader.reader-types
        lazy-map.core}
     '#{cljs.core
        com.cognitect.transit
        com.cognitect.transit.delimiters
        com.cognitect.transit.handlers
        com.cognitect.transit.util
        com.cognitect.transit.caching
        com.cognitect.transit.types
        com.cognitect.transit.eq
        com.cognitect.transit.impl.decoder
        com.cognitect.transit.impl.reader
        com.cognitect.transit.impl.writer})
    name))

;; An atom to keep track of things we've already loaded
(def loaded (atom #{}))

(defn load?
  [name macros]
  (let [do-not-load (or (@loaded name)
                      (skip-load? name macros))]
    (swap! loaded conj name)
    (not do-not-load)))

(defn make-load-fn
  "Makes a load function that will read from a sequence of src-paths
  using a supplied read-file-fn. It returns a cljs.js-compatible
  *load-fn*.

  Read-file-fn is a 2-arity function (fn [filename source-cb] ...) where
  source-cb is itself a function (fn [source] ...) that needs to be called
  with the source of the library (as string)."
  [src-paths read-file-fn]
  (fn [{:keys [name macros path]} cb]
    ;(prn name)
    (if (load? name macros)
      (if (re-matches #"^goog/.*" path)
        (load-goog name cb)
        (read-some (filenames-to-try src-paths macros path) read-file-fn cb))
      (cb {:source ""
           :lang   :js}))))

;; Simple REPL

(defn read-eval-print-loop
  [src-paths]
  (let [cmd (prompt-read (replumb/get-prompt))]
    (replumb/read-eval-call
     (merge
      (replumb/options :nodejs (make-load-fn src-paths osa-read-file)))
     #(println (replumb/result->string %))
     cmd)
    (recur src-paths)))

(defn arg->src-paths
  [arg]
  (string/split arg #":"))

(defn -main [& args]
  (read-eval-print-loop
    (if-not (empty? args)
      (-> (first args)
        arg->src-paths)
      ["./"])))

(set! js/global.run #(apply -main %))
