(ns tolitius.boot-check
  {:boot/export-tasks true}
  (:require [tolitius.yagni :as ty]
            [boot.core :as core :refer [deftask user-files tmp-file set-env! get-env]]
            [boot.pod  :as pod]))

(def kibit-deps
  '[[jonase/kibit "0.1.2"]
    [org.clojure/tools.cli "0.3.3"]])

(def pod-deps
  '[[org.clojure/tools.namespace "0.2.11" :exclusions [org.clojure/clojure]]])

(defn fileset->paths [fileset]
  (->> fileset
       user-files
       (mapv (comp #(.getAbsolutePath %) tmp-file))))

(defn bootstrap [fresh-pod]
  (doto fresh-pod
    (pod/with-eval-in
     (require '[clojure.java.io :as io]
              '[clojure.tools.namespace.find :refer [find-namespaces-in-dir]])

     (defn all-ns* [& dirs]
       (distinct (mapcat #(find-namespaces-in-dir (io/file %)) dirs))))))

(defn ppool [deps init]
  (let [pod-deps (update-in (core/get-env) [:dependencies]
                            into deps)
        pool (pod/pod-pool pod-deps :init init)]
    (core/cleanup (pool :shutdown))
  pool))

(defn- kibit-it [pod-pool fileset & args]
  (let [worker-pod (pod-pool :refresh)
        namespaces (pod/with-eval-in worker-pod
                     (all-ns* ~@(->> fileset
                                     core/input-dirs
                                     (map (memfn getPath)))))
        sources (fileset->paths fileset)]
    (pod/with-eval-in worker-pod
      (boot.util/dbug (str "kibit is about to look at: -- " '~sources " --"))
      (require '[kibit.driver :as kibit])
      (doseq [ns '~namespaces] (require ns))
      (let [problems# (apply kibit.driver/run '~sources nil '~args)]   ;; nil for "rules" which would expand to all-rules,
        (if-not (zero? (count problems#))
          (throw (ex-info "kibit found some problems: " {:problems (set problems#)}))
          (boot.util/info "\nlatest report from kibit.... [You Rock!]\n"))))))

(defn- yagni-it [pod-pool fileset & args]
  (let [worker-pod (pod-pool :refresh)
        sources (fileset->paths fileset)]
    (pod/with-eval-in worker-pod
      (boot.util/dbug (str "yagni is about to look at: -- " '~sources " --"))
      (require '[yagni.core :as yagni])
      (require '[tolitius.yagni :as tyagni])
      (let [graph# (binding [*ns* (the-ns *ns*)] 
                     (yagni/construct-reference-graph '~sources))
            problems# (tyagni/check graph#)]
        (if (seq problems#)
          (tyagni/report problems#)
          (boot.util/info "\nlatest report from yagni.... [You Rock!]\n"))))))

(deftask with-kibit
  "Static code analyzer for Clojure, ClojureScript, cljx and other Clojure variants.

  This task will run all the kibit checks within a pod.

  At the moment it takes no arguments, but behold..! it will. (files, rules, reporters, etc..)"
  ;; [f files FILE #{sym} "the set of files to check."]      ;; TODO: convert these to "tmp-dir/file"
  []
  (let [pod-pool (ppool (concat pod-deps kibit-deps) bootstrap)]
    (core/with-pre-wrap fileset
      (kibit-it pod-pool fileset) ;; TODO with args
      fileset)))

(deftask with-yagni
  "Static code analyzer for Clojure that helps you find unused code in your applications and libraries.

  This task will run all the yagni checks within a pod.

  At the moment it takes no arguments, but behold..! it will."
  []
  (let [pod-pool (ppool (concat pod-deps ty/yagni-deps) bootstrap)]
    (core/with-pre-wrap fileset
      (yagni-it pod-pool fileset) ;; TODO with args
      fileset)))
