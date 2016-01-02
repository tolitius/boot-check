(ns tolitius.boot.helper
  (:require [boot.core :as core]
            [boot.pod  :as pod]))

(defn tmp-dir-paths [fs]
  (mapv #(.getAbsolutePath %) 
        (core/input-dirs fs)))

(defn fileset->paths [fileset]
  (->> fileset
       core/user-files
       (mapv (comp #(.getAbsolutePath %) core/tmp-file))))

(defn make-pod-pool [deps init]
  (let [pod-deps (update-in (core/get-env) [:dependencies]
                            into deps)
        pool (pod/pod-pool pod-deps :init init)]
    (core/cleanup (pool :shutdown))
  pool))
