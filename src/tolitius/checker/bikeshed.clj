(ns tolitius.checker.bikeshed
  (:require [tolitius.boot.helper :refer :all]
            [boot.pod :as pod]))

(def bikeshed-deps
  '[[lein-bikeshed "0.4.1" :exclusions [org.clojure/tools.cli 
                                        org.clojure/tools.namespace]]])

(defn check [pod-pool fileset & args]
  (let [worker-pod (pod-pool :refresh)]
    (pod/with-eval-in worker-pod
      (require '[bikeshed.core])
      (let [sources# ~(tmp-dir-paths fileset)
            _ (boot.util/dbug (str "bikeshed is about to look at: -- " sources# " --"))
            problems# (apply bikeshed.core/bikeshed {:source-paths sources#} [~@args])]
        (if problems#
          (do
            (boot.util/warn (str "\nWARN: bikeshed found some problems ^^^ \n"))
            {:errors problems#})
          (boot.util/info "\nlatest report from bikeshed.... [You Rock!]\n"))))))
