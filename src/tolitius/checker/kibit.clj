(ns tolitius.checker.kibit
  (:require [tolitius.boot.helper :refer :all]
            [boot.core :as core]
            [tolitius.core.check :as ch]
            [boot.pod  :as pod]))

(def kibit-deps
  '[[jonase/kibit "0.1.8"]
    [org.clojure/tools.cli "0.3.3"]])

;;Kibit does not report file :(  - it is a bug. Next version of kibit will support that.
(defn normalise-issue [warning]
  (let [{:keys [expr alt line column file]} warning
        msg (str "Consider changing [ " (pr-str expr) " ] with [ " (pr-str alt) " ]")
        linter "kibit"]
    (ch/issue :kibit linter msg (ch/coords file line column) nil)))

(defn check [pod-pool fileset & args]
  (let [worker-pod (pod-pool :refresh)
        namespaces (pod/with-eval-in worker-pod
                     (all-ns* ~@(->> fileset
                                     core/input-dirs
                                     (map (memfn getPath)))))
        sources (fileset->paths fileset)]
    (pod/with-eval-in worker-pod
      (boot.util/dbug (str "kibit is about to look at: -- " '~sources " --"))
      (require '[kibit.driver :as kibit]
               '[tolitius.checker.kibit :as checker])
      (doseq [ns '~namespaces] (require ns))
      (let [problems# (apply kibit.driver/run '~sources nil '~args)]   ;; nil for "rules" which would expand to all-rules,
        (if-not (zero? (count problems#))
          (boot.util/warn (str "\nWARN: kibit found some problems: \n\n" {:problems (set problems#)} "\n"))
          (boot.util/info "\nlatest report from kibit.... [You Rock!]\n"))
          {:warnings (or (mapv checker/normalise-issue (vec problems#)) [])}))))
