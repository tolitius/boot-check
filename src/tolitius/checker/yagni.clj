(ns tolitius.checker.yagni
  (:require [boot.core :refer [get-env]]
            [boot.pod :as pod]
            [tolitius.boot.helper :refer :all]
            [clojure.string :as s]))

(def yagni-deps
  '[[venantius/yagni "0.1.4" :exclusions [org.clojure/clojure]]])

(defn- pp [s]
  (s/join "\n" s))

(defn check-graph [find-family g]
  (let [{:keys [children parents]} (find-family @g)]
    (cond-> {}
      (seq parents) (assoc :no-refs (set parents))
      (seq children) (assoc :no-parent-refs (set children)))))

(defn report [{:keys [no-refs no-parent-refs]}]
  (when no-refs
    (boot.util/warn (str "\nWARN: could not find any references to the following:\n\n" (pp no-refs) "\n")))
  (when no-parent-refs
    (boot.util/warn (str "\nWARN: the following have references to them, but their parents do not:\n\n" (pp no-parent-refs) "\n"))))

(defn check [pod-pool fileset & args]
  (let [worker-pod (pod-pool :refresh)
        sources (fileset->paths fileset)]
    (pod/with-eval-in worker-pod
      (boot.util/dbug (str "yagni is about to look at: -- " '~sources " --"))
      (require '[yagni.core :as yagni]
               '[yagni.graph :refer [find-children-and-parents]]
               '[tolitius.checker.yagni :refer [check-graph report]])
      (let [graph# (binding [*ns* (the-ns *ns*)] 
                     (yagni/construct-reference-graph '~sources))
            problems# (check-graph find-children-and-parents graph#)]
        (if (seq problems#)
          (report problems#)
          (boot.util/info "\nlatest report from yagni.... [You Rock!]\n"))))))
