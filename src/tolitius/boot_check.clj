(ns tolitius.boot-check
  {:boot/export-tasks true}
  (:require [boot.core :as core :refer [deftask user-files set-env! get-env]]
            [boot.pod  :as pod]))

(def kibit-dep
  '[jonase/kibit "0.1.2"])

(def tn-dep
  '[org.clojure/tools.namespace "0.2.11" :exclusions [org.clojure/clojure]])

(defn init [fresh-pod]
  (doto fresh-pod
    (pod/with-eval-in
     (require '[clojure.java.io :as io]
              '[clojure.tools.namespace.find :refer [find-namespaces-in-dir]])

     (defn all-ns* [& dirs]
       (distinct (mapcat #(find-namespaces-in-dir (io/file %)) dirs))))))

(deftask with-kibit
  "Static code analyzer for Clojure, ClojureScript, cljx and other Clojure variants.

  This task will run all the kibit checks within a pod.

  At the moment it takes no arguments, but behold..! it will. (files, rules, reporters, etc..)"
  ;; [f files FILE #{sym} "the set of files to check."]      ;; TODO: convert these to "tmp-dir/file"
  []
  (let [pod-deps (update-in (core/get-env) [:dependencies]
                            into [tn-dep kibit-dep])
        worker-pods (pod/pod-pool pod-deps :init (partial init))]
    (core/cleanup (worker-pods :shutdown))
    (core/with-pre-wrap fileset
      (let [worker-pod (worker-pods :refresh)
            namespaces (pod/with-eval-in worker-pod
                         (all-ns* ~@(->> fileset
                                         core/input-dirs
                                         (map (memfn getPath)))))
            sources (->> fileset
                         user-files
                         (mapv (comp #(.getAbsolutePath %) core/tmp-file)))]
        (pod/with-eval-in worker-pod
          (boot.util/dbug (str "kibit is about to look at: -- " '~sources " --"))
          (require '[kibit.driver :as kibit])
          (doseq [ns '~namespaces] (require ns))
          (let [problems (apply kibit.driver/run '~sources nil [])]   ;; nil for "rules" which would expand to all-rules,
                                                                      ;; [] for args that are to come
            (if-not (zero? (count problems))
              (boot.util/fail (str "\nkibit found some problems: " (set problems) "\n"))
              (boot.util/info "latest report from kibit.... [You Rock!]\n"))))
        fileset))))
