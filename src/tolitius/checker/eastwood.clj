(ns tolitius.checker.eastwood
  (:require [tolitius.boot.helper :refer :all]
            [boot.core :as core :refer [deftask user-files tmp-file set-env! get-env]]
            [boot.pod  :as pod]))

(def eastwood-deps
  '[[jonase/eastwood "0.2.3" :exclusions [org.clojure/clojure]]])

(defn check [pod-pool fileset & args]
  (let [worker-pod (pod-pool :refresh)
        namespaces (pod/with-eval-in worker-pod
                     (all-ns* ~@(->> fileset
                                     core/input-dirs
                                     (map (memfn getPath)))))
        sources (tmp-dir-paths fileset)]
    (pod/with-eval-in worker-pod
      (boot.util/dbug (str "eastwood is about to look at: -- " '~sources " --"))
      (require '[eastwood.lint :as eastwood])
      ;; (require '[boot.core :as core])
      ;; (boot.core/load-data-readers!)
      (doseq [ns '~namespaces] (require ns))
      (let [{:keys [some-warnings]} (eastwood/eastwood {:source-paths '~sources
                                                        ;; :debug #{:ns}
                                                        })]
        (if some-warnings
          (boot.util/warn (str "\nWARN: eastwood found some problems ^^^ \n\n"))
          (boot.util/info "\nlatest report from eastwood.... [You Rock!]\n"))))))
