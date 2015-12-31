(ns tolitius.yagni
  (:require [boot.core :refer [get-env]]
            [boot.pod :as pod]
            [clojure.string :as s]))

(def yagni-deps
  '[[venantius/yagni "0.1.4" :exclusions [org.clojure/clojure]]])

(defn- pp [s]
  (s/join "\n" s))

(defn check [g]
  (let [worker (pod/make-pod (update-in (get-env) [:dependencies]   ;; TODO: should most likely come from the existing pod pool
                                        into yagni-deps))
        graph @g]
    (pod/with-eval-in worker
      (require '[yagni.graph :refer [find-children-and-parents]])
      (let [{:keys [children parents]} (find-children-and-parents '~graph)]
        (cond-> {}
          (seq parents) (assoc :no-refs (set parents))
          (seq children) (assoc :no-parent-refs (set children)))))))

(defn report [{:keys [no-refs no-parent-refs]}]
  (when no-refs
    (boot.util/warn (str "\n\nWARN: could not find any references to the following:\n\n" (pp no-refs))))
  (when no-parent-refs
    (boot.util/warn (str "\n\nWARN: the following have references to them, but their parents do not:\n\n" (pp no-parent-refs)))))
