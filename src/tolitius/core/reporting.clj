(ns tolitius.core.reporting)

(defmulti report (fn [issues options] (:reporter options)))

(defmulti report-extension (fn [options] (:reporter options)))
