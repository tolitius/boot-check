(ns test.with-eastwood
  (:require [clojure.test :refer [deftest is]]))

(defn nested-def []
  (def a 42)
  a)

(deftest always-true
  (is (= 42 42)))
