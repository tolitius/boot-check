(ns tolitius.core.reporting)
            
(defmulti report (fn [issues options] (:reporter options)))
