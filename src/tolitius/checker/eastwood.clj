(ns tolitius.checker.eastwood
  (:require [tolitius.boot.helper :refer :all]
            [boot.pod  :as pod]))

(def eastwood-deps
  '[[jonase/eastwood "0.2.4" :exclusions [org.clojure/clojure]]])

(defn check [pod-pool fileset options & args]
  (let [worker-pod (pod-pool :refresh)
        exclude-linters (:exclude-linters options)]
    (pod/with-eval-in worker-pod
      (require '[eastwood.lint :as eastwood])
      ;; ~(boot.core/load-data-readers!)
      (let [sources# #{~@(tmp-dir-paths fileset)}
            _ (boot.util/dbug (str "eastwood is about to look at: -- " sources# " --"))
            {:keys [some-warnings] :as checks} (eastwood/eastwood {:source-paths sources#
                                                                   :exclude-linters ~exclude-linters
                                                                   ;; :debug #{:ns}
                                                                   })]
        (if some-warnings
          (do
            (boot.util/warn (str "\nWARN: eastwood found some problems ^^^ \n\n"))
            {:errors (eastwood/eastwood-core (eastwood/last-options-map-adjustments  ;; TODO rerun to get the actual errors, but otherwise need to rewrite eastwood/eastwood
                                               {:source-paths sources#}))})
          (boot.util/info "\nlatest report from eastwood.... [You Rock!]\n"))))))
