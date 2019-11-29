(ns tolitius.checker.bikeshed
  (:require [tolitius.boot.helper :refer :all]
            [tolitius.core.check :as ch]
            [boot.pod :as pod]))

(def bikeshed-deps
  '[[org.clojure/clojure "1.10.1"]
    [lein-bikeshed "0.5.2" :exclusions [org.clojure/tools.cli
                                        org.clojure/tools.namespace]]])

(defn to-warning [problems]
  (when problems
    [(ch/issue :bikeshed :summary (str "Following bikeshed checks failed : " (clojure.string/join ", " problems)) (ch/coords " ? " " ? " " ? ") nil)]))

(defn check [pod-pool fileset & args]
  (let [worker-pod (pod-pool :refresh)]
    (pod/with-eval-in worker-pod
      (require '[bikeshed.core]
               '[tolitius.checker.bikeshed :as checker])
      (let [sources# ~(tmp-dir-paths fileset)
            _ (boot.util/dbug (str "bikeshed is about to look at: -- " sources# " --"))
            args# (update ~@args :check? #(merge % {}))
            problems# (bikeshed.core/bikeshed {:source-paths sources#} args#)]
        (if problems#
          (boot.util/warn (str "\nWARN: bikeshed found some problems ^^^ \n"))
          (boot.util/info "\nlatest report from bikeshed.... [You Rock!]\n"))
        {:warnings (or (checker/to-warning problems#) [])}))))
