(ns tolitius.boot-check
  {:boot/export-tasks true}
  (:require [tolitius.checker.yagni :as yagni :refer [yagni-deps]]
            [tolitius.checker.kibit :as kibit :refer [kibit-deps]]
            [tolitius.checker.eastwood :as eastwood :refer [eastwood-deps]]
            [tolitius.checker.bikeshed :as bikeshed :refer [bikeshed-deps]]
            [tolitius.boot.helper :refer :all]
            [boot.core :as core :refer [deftask]]
            [boot.pod  :as pod]))

(def pod-deps
  '[[org.clojure/tools.namespace "0.2.11" :exclusions [org.clojure/clojure]]])

(defn bootstrap [fresh-pod]
  (doto fresh-pod
    (pod/with-eval-in
     (require '[clojure.java.io :as io]
              '[clojure.tools.namespace.find :refer [find-namespaces-in-dir]]
              '[tolitius.boot.helper :refer :all])

     (defn all-ns* [& dirs]
       (distinct (mapcat #(find-namespaces-in-dir (io/file %)) dirs))))))

(defn with-throw [f msg throw?]
  (let [{:keys [errors]} (f)]
    (when (and errors throw?)
      (throw (ex-info msg
                      {:causes errors})))))

(deftask with-kibit
  "Static code analyzer for Clojure, ClojureScript, cljx and other Clojure variants.

  This task will run all the kibit checks within a pod.

  At the moment it takes no arguments, but behold..! it will. (files, rules, reporters, etc..)"
  ;; [f files FILE #{sym} "the set of files to check."]      ;; TODO: convert these to "tmp-dir/file"
  [t throw-on-errors bool "throw an exception if the check does not pass"]
  (let [pod-pool (make-pod-pool (concat pod-deps kibit-deps) bootstrap)]
    (core/with-pre-wrap fileset
      (with-throw #(kibit/check pod-pool fileset)          ;; TODO with args
                  "kibit checks fail"
                  throw-on-errors)
      fileset)))

(deftask with-yagni
  "Static code analyzer for Clojure that helps you find unused code in your applications and libraries.

  This task will run all the yagni checks within a pod."
  [o options OPTIONS edn "yagni options EDN map"
   t throw-on-errors bool "throw an exception if the check does not pass"]
  (let [pod-pool (make-pod-pool (concat pod-deps yagni-deps) bootstrap)]
    (core/with-pre-wrap fileset
      (with-throw #(yagni/check pod-pool fileset options)  ;; TODO with args
                  "yagni checks fail"
                  throw-on-errors)
      fileset)))

(deftask with-eastwood
  "Clojure lint tool that uses the tools.analyzer and tools.analyzer.jvm libraries to inspect namespaces and report possible problems

  This task will run all the eastwood checks within a pod.

  At the moment it takes no arguments, but behold..! it will. (linters, namespaces, etc.)"
  ;; [f files FILE #{sym} "the set of files to check."]      ;; TODO: convert these to "tmp-dir/file"
  [o options OPTIONS edn "eastwood options EDN map"
   t throw-on-errors bool "throw an exception if the check does not pass"]
  (let [pod-pool (make-pod-pool (concat pod-deps eastwood-deps) bootstrap)]
    (core/with-pre-wrap fileset
      (with-throw #(eastwood/check pod-pool fileset options)
                  "eastwood checks fail"
                  throw-on-errors)
      fileset)))

(deftask with-bikeshed
  "This task is backed by 'lein-bikeshed' which is designed to tell you your code is bad, and that you should feel bad

  This task will run all the bikeshed checks within a pod.

  At the moment it takes no arguments, but behold..! it will. ('-m, --max-line-length', etc.)"
  ;; [f files FILE #{sym} "the set of files to check."]       ;; TODO: convert these to "tmp-dir/file"
  [o options OPTIONS edn "bikeshed options EDN map"
   t throw-on-errors bool "throw an exception if the check does not pass"]
  (let [pod-pool (make-pod-pool (concat pod-deps bikeshed-deps) bootstrap)]
    (core/with-pre-wrap fileset
      (with-throw #(bikeshed/check pod-pool fileset options)  ;; TODO with args
                  "bikeshed checks fail"
                  throw-on-errors)
      fileset)))
