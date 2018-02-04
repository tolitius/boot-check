(ns tolitius.core.reporting
  (:require [boot.core :as c]
            [clojure.java.io :as io]))

(defmulti report (fn [issues options] (:reporter options)))
