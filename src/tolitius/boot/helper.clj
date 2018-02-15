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

(defn load-issue-related-file-part [inputs issue offset-lines]
  (when-let [file (-> issue :coords :file)]
    (let [line (-> issue :coords :line)
          start (- line offset-lines)
          end  (+ line offset-lines)
          input-file (first (filter #(.endsWith % file) inputs))]
      (with-open [rdr (clojure.java.io/reader input-file)]
        (doall (filter #(and (<= start (first %)) (>= end (first %))) (map-indexed (fn [i v] [(inc i) v]) (line-seq rdr))))))))
