(ns test.with-kibit)

(defn when-vs-if []
  (if 42 42 nil))

(defn vec-vs-into []
  (into [] 42))
