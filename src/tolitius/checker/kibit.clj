(ns tolitius.checker.kibit
  (:require [tolitius.boot.helper :refer :all]
            [boot.core :as core]
            [boot.pod  :as pod]))

(def kibit-deps
  '[[jonase/kibit "0.1.3"]
    [org.clojure/tools.cli "0.3.3"]])

(defn check [pod-pool fileset & args]
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
          (do
            (boot.util/warn (str "\nWARN: kibit found some problems: \n\n" {:problems (set problems#)} "\n"))
            {:errors problems#})
          (boot.util/info "\nlatest report from kibit.... [You Rock!]\n"))))))
