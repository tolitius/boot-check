(ns tolitius.checker.yagni
  (:require [boot.pod :as pod]
            [tolitius.boot.helper :refer :all]
            [tolitius.core.check :as ch]
            [clojure.string :as s]))

(def yagni-deps
  '[[venantius/yagni "0.1.7" :exclusions [org.clojure/clojure]]])

;; yagni implementation (07/26/2016) is coupled with a lein specific file
(defonce entry-points-file ".lein-yagni")

(defn- pp [s]
  (s/join "\n" s))

(defn- namespace->file [namespace]
  (str (clojure.string/replace (clojure.string/replace (str namespace) #"/.*" "") #"\." "/") ".clj"))

(defn- error-desc [family varname]
  (cond
    (= family :no-refs) (str  "Could not find any reference to Var " varname)
    (= family :no-parent-refs) (str "Var " varname " is referenced by unused code")))

(defn to-issue [namespace family]
  (ch/issue :yagni family (error-desc family (str namespace)) (ch/coords (namespace->file namespace) 0 0) nil))

(defn check-graph [find-family g]
  (let [{:keys [children parents]} (find-family @g)]
    (concat (mapv #(to-issue % :no-parent-refs) (vec children))
            (mapv #(to-issue % :no-refs) (vec parents)))))

(defn report [{:keys [no-refs no-parent-refs]}]
  (when no-refs
    (boot.util/warn (str "\nWARN: could not find any references to the following:\n\n" (pp no-refs) "\n")))
  (when no-parent-refs
    (boot.util/warn (str "\nWARN: the following have references to them, but their parents do not:\n\n" (pp no-parent-refs) "\n"))))

(defn create-entry-points [entry-points]
  (when entry-points
    (->> (interpose "\n" entry-points)
         (apply str)
         (spit entry-points-file))))

(defn check [pod-pool fileset {:keys [entry-points]}]
  (let [worker-pod (pod-pool :refresh)
        sources (fileset->paths fileset)]
    (pod/with-eval-in worker-pod
      (boot.util/dbug (str "yagni is about to look at: -- " '~sources " --"
                        (if '~entry-points
                          (str "\nwith entry points -- " '~entry-points " --")
                          "\nwith no entry points")))
      (require '[yagni.core :as yagni]
               '[yagni.graph :refer [find-children-and-parents]]
               '[tolitius.checker.yagni :refer [check-graph report create-entry-points]])
      (let [graph# (binding [*ns* (the-ns *ns*)]
                     (create-entry-points '~entry-points)
                     (yagni/construct-reference-graph '~sources))
            problems# (check-graph find-children-and-parents graph#)]
        (if problems#
          (boot.util/info "\nlatest report from yagni.... Yagni found some problems!\n")
          (boot.util/info "\nlatest report from yagni.... [You Rock!]\n"))
        {:warnings (or problems# [])}))))
