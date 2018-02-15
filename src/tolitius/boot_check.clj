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

(def ^:const default-report-file-name "boot-check-report")

(def pod-deps
  '[[org.clojure/tools.namespace "0.2.11" :exclusions [org.clojure/clojure]]])

(defn- store-tmp-file [fileset tmpdir content filename]
  (core/empty-dir! tmpdir)
  (let [content-file (io/file tmpdir filename)]
    (doto content-file
      io/make-parents
      (spit content))
    (let [new (core/add-source fileset tmpdir)]
      (core/commit! new))))

(defn- load-issues [fileset]
  (if-let [issues (->> fileset core/input-files (core/by-name [interim-report-data-file]) first)]
    (read-string (-> issues core/tmp-file slurp))
    []))

(defn- append-issues [fileset tmpdir issues]
  (let [content (concat (load-issues fileset) issues)
        str-content (pr-str content)]
    (store-tmp-file fileset tmpdir str-content interim-report-data-file)))

(defn- start-date [fileset tmpdir]
  (if-let [datefile (->> fileset core/input-files (core/by-name ["timestamp"]) first)]
    fileset
    (store-tmp-file fileset tmpdir (.format (java.text.SimpleDateFormat. "yyyy_MM_dd_HH_mm_ss") (java.util.Date.)) "timestamp")))

(defn- check-start-date [fileset]
  (when-let [datefile (->> fileset core/input-files (core/by-name ["timestamp"]) first)]
    (-> datefile core/tmp-file slurp)))

(defn- write-report [fileset tmpdir report filename]
  (store-tmp-file fileset tmpdir report filename))

(defn- do-report [fileset tmpdir issues options]
  (let [reporter         (core/get-env :boot-check-reporter :html)
        report-path      (core/get-env :report-path  "")
        report-file-name (core/get-env :report-file-name default-report-file-name)
        skip-time?       (core/get-env :report-skip-time? false)
        fileset          (append-issues (start-date fileset tmpdir) tmpdir issues)
        refreshed        (load-issues fileset)
        report-content   (r/report refreshed (assoc options :reporter reporter))]
    (let [date-suffix (if skip-time? "" (str "." (check-start-date fileset)))
          dated-report-file-name (str report-file-name date-suffix ".html")]
      (boot.util/dbug (str "\nWriting report to current directory: " report-path dated-report-file-name "...\n"))
      (doto
        (io/file (str report-path dated-report-file-name))
        io/make-parents
        (spit report-content)))
    (boot.util/dbug "\nWriting report to boot fileset TEMP directory...\n")
    (write-report fileset tmpdir report-content report-file-name)))

(defn bootstrap [fresh-pod]
  (doto fresh-pod
    (pod/with-eval-in
     (require '[clojure.java.io :as io]
              '[clojure.tools.namespace.find :refer [find-namespaces-in-dir]]
              '[tolitius.boot.helper :refer :all])

     (defn all-ns* [& dirs]
       (distinct (mapcat #(find-namespaces-in-dir (io/file %)) dirs))))))

(defn- process-results [fileset tmpdir f msg throw? options]
  (when-let [{:keys [warnings]} (f)]
    (when (and (seq warnings) throw?)
      (boot.util/warn-deprecated (str "\nWARN: throw-on-errors OPTION should be replaced by adding throw-on-errors TASK at the end of pipeline!^^^ \n"))
      (throw (ex-info msg {:causes warnings})))
    (if (true? (:gen-report options))
      (do-report fileset tmpdir warnings options)
      fileset)))

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
      (process-results fileset tmpdir
                       #(kibit/check pod-pool fileset)  ;; TODO with args
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
      (process-results fileset tmpdir
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
      (process-results fileset tmpdir
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
      (process-results fileset tmpdir
                       #(bikeshed/check pod-pool fileset options)  ;; TODO with args
                       "bikeshed checks fail"
                       throw-on-errors options))))

(deftask throw-on-errors
  "This task provides caller with exception when some of code checkers reports warnings.

  Using this task makes sense when You want to skip later tasks within the pipline as your

  rigorous policy assumes every line of code to be perfect ;-)

  When using this task You decide when to throw an exception. You may want to throw exception after

  particular checker or after all checkers has completed"
  []
  (core/with-pre-wrap fileset
    (when-let [issues (load-issues fileset)]
      (when (seq issues)
        (throw (ex-info "Some of code checkers have failed." {:causes issues}))))
    fileset))
