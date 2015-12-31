(ns test.with-yagni
  (:require [test.with-kibit]))

(defn func [] true)

(def notafunc false)

(defn func-the-second [] notafunc)

(defn other-func [] (func))
