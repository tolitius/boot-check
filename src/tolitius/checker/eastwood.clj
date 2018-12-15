(ns tolitius.checker.eastwood
  (:require [tolitius.boot.helper :refer :all]
            [tolitius.core.check :as ch]
            [boot.pod  :as pod]))

(def eastwood-deps
  '[[jonase/eastwood "0.3.3" :exclusions [org.clojure/clojure]]])

(defn eastwood-linting-callback [files handle-issue options]
  (fn [{:keys [warn-data kind] :as data}]
    (when (= :lint-warning kind)
      (let [exclude-linters (:exclude-linters options)
            {:keys [file line column linter msg form]} warn-data]
        (if-not (some #{linter} exclude-linters)
          (let [issue (ch/issue :eastwood linter msg (ch/coords file line column) nil)]
            (if-let [warn-contents (load-issue-related-file-part files issue 5)]
              (handle-issue (assoc issue :snippet warn-contents))
              (handle-issue issue))))))))

(defn check [pod-pool fileset options & args]
  (let [worker-pod (pod-pool :refresh)
        inputs (fileset->paths fileset)
        exclude-linters (:exclude-linters options)]
    (pod/with-eval-in worker-pod
      (require '[eastwood.lint :as eastwood]
               '[tolitius.checker.eastwood :as checker]
               '[tolitius.core.check :as ch])
      (let [sources# #{~@(tmp-dir-paths fileset)}
            _ (boot.util/dbug (str "eastwood is about to look at: -- " sources# " --"))
            {:keys [some-warnings] :as checks} (eastwood/eastwood {:source-paths sources#
                                                                   :exclude-linters ~exclude-linters })

            issues# (atom #{})]
        (if some-warnings
          (do
            (boot.util/warn (str "\nWARN: eastwood found some problems ^^^ \n\n"))
            (eastwood/eastwood-core (eastwood/last-options-map-adjustments  ;; TODO rerun to get the actual errors, but otherwise need to rewrite eastwood/eastwood
                                        {:source-paths sources#
                                         :callback (checker/eastwood-linting-callback ~inputs #(swap! issues# conj %) ~options)})))
          (boot.util/info "\nlatest report from eastwood.... [You Rock!]\n"))
        {:warnings (or (vec @issues#) [])}))))
