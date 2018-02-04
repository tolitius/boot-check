(ns tolitius.boot-check
  {:boot/export-tasks true}
  (:require [tolitius.checker.yagni :as yagni :refer [yagni-deps]]
            [tolitius.checker.kibit :as kibit :refer [kibit-deps]]
            [tolitius.checker.eastwood :as eastwood :refer [eastwood-deps]]
            [tolitius.checker.bikeshed :as bikeshed :refer [bikeshed-deps]]
            [tolitius.boot.helper :refer :all]
            [tolitius.core.reporting :as r]
            [tolitius.core.check :as check]
            [boot.core :as core :refer [deftask]]
            [clojure.java.io :as io]
            [boot.pod  :as pod]))

(def ^:const interim-report-data-file "interim-data")

(def pod-deps
  '[[org.clojure/tools.namespace "0.2.11" :exclusions [org.clojure/clojure]]])

(defn store-tmp-file [fileset tmpdir content filename]
  (core/empty-dir! tmpdir)
  (let [content-file (io/file tmpdir filename)]
    (doto content-file
      io/make-parents
      (spit content))
    (let [new (-> fileset (core/add-source tmpdir))]
      (core/commit! new))))

(defn load-issues [fileset]
  (if-let [issues (->> fileset core/input-files (core/by-name [interim-report-data-file]) first)]
    (read-string (-> issues core/tmp-file slurp))
    []))

(defn append-issues [fileset tmpdir issues]
  (let [content (concat (load-issues fileset) issues)
        str-content (pr-str content)]
    (store-tmp-file fileset tmpdir str-content interim-report-data-file)))

(defn write-report [fileset tmpdir report]
  (store-tmp-file fileset tmpdir report "report.html"))

(defn- do-report [fileset tmpdir issues options]
  (if (not (nil? (:reporter options)))
    (let [fileset (append-issues fileset tmpdir issues)
          refreshed (load-issues fileset)]
      (let [report-content (r/report refreshed options)]
        (boot.util/info "\nGenerating report...\n")
        (write-report fileset tmpdir report-content)))
    fileset))    

(defn bootstrap [fresh-pod]
  (doto fresh-pod
    (pod/with-eval-in
     (require '[clojure.java.io :as io]
              '[clojure.tools.namespace.find :refer [find-namespaces-in-dir]]
              '[tolitius.boot.helper :refer :all])

     (defn all-ns* [& dirs]
       (distinct (mapcat #(find-namespaces-in-dir (io/file %)) dirs))))))

(defn with-report [fileset tmpdir f msg throw? options]
  (when-let [{:keys [warnings]} (f)]
    (when throw?
      (boot.util/warn-deprecated (str "\nWARN: throw-on-errors OPTION should be replaced by adding throw-on-errors TASK at the end of pipeline!^^^ \n"))
      (throw (ex-info msg {:causes warnings})))
    (do-report fileset tmpdir warnings options)))

(deftask with-kibit
  "Static code analyzer for Clojure, ClojureScript, cljx and other Clojure variants.

  This task will run all the kibit checks within a pod.

  At the moment it takes no arguments, but behold..! it will. (files, rules, reporters, etc..)"
  ;; [f files FILE #{sym} "the set of files to check."]      ;; TODO: convert these to "tmp-dir/file"
  [o options OPTIONS edn "kibit options EDN map"
   t throw-on-errors bool "throw an exception if the check does not pass"]
  (let [pod-pool (make-pod-pool (concat pod-deps kibit-deps) bootstrap)
        tmpdir (core/tmp-dir!)]
    (core/with-pre-wrap fileset
      (with-report fileset tmpdir
                   #(kibit/check pod-pool fileset)          ;; TODO with args
                   "kibit checks fail"
                   throw-on-errors options))))

(deftask with-yagni
  "Static code analyzer for Clojure that helps you find unused code in your applications and libraries.

  This task will run all the yagni checks within a pod."
  [o options OPTIONS edn "yagni options EDN map"
   t throw-on-errors bool "throw an exception if the check does not pass"]
  (let [pod-pool (make-pod-pool (concat pod-deps yagni-deps) bootstrap)
        tmpdir (core/tmp-dir!)]
    (core/with-pre-wrap fileset
      (with-report fileset tmpdir
                   #(yagni/check pod-pool fileset options)  ;; TODO with args
                   "yagni checks fail"
                   throw-on-errors options))))

(deftask with-eastwood
  "Clojure lint tool that uses the tools.analyzer and tools.analyzer.jvm libraries to inspect namespaces and report possible problems

  This task will run all the eastwood checks within a pod.

  At the moment it takes no arguments, but behold..! it will. (linters, namespaces, etc.)"
  ;; [f files FILE #{sym} "the set of files to check."]      ;; TODO: convert these to "tmp-dir/file"
  [o options OPTIONS edn "eastwood options EDN map"
   t throw-on-errors bool "throw an exception if the check does not pass"]
  (let [pod-pool (make-pod-pool (concat pod-deps eastwood-deps) bootstrap)
        tmpdir (core/tmp-dir!)]
    (core/with-pre-wrap fileset
      (with-report fileset tmpdir
                   #(eastwood/check pod-pool fileset options)
                   "eastwood checks fail"
                   throw-on-errors options))))

(deftask with-bikeshed
  "This task is backed by 'lein-bikeshed' which is designed to tell you your code is bad, and that you should feel bad

  This task will run all the bikeshed checks within a pod.

  At the moment it takes no arguments, but behold..! it will. ('-m, --max-line-length', etc.)"
  ;; [f files FILE #{sym} "the set of files to check."]       ;; TODO: convert these to "tmp-dir/file"
  [o options OPTIONS edn "bikeshed options EDN map"
   t throw-on-errors bool "throw an exception if the check does not pass"]
  (let [pod-pool (make-pod-pool (concat pod-deps bikeshed-deps) bootstrap)
        tmpdir (core/tmp-dir!)]
    (core/with-pre-wrap fileset
      (with-report fileset tmpdir
                   #(bikeshed/check pod-pool fileset options)  ;; TODO with args
                   "bikeshed checks fail"
                   throw-on-errors options))))
