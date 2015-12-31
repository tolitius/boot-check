(ns test.with-yagni
  (:gen-class)
  (:require [test.with-kibit]))

(defn func [] (test.with-kibit/when-vs-if))

(def notafunc false)

(defn func-the-second [] notafunc)

(defn other-func [] (func))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
