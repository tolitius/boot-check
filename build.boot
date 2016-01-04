(def +version+ "0.1.1-SNAPSHOT")

(set-env!
  :source-paths #{"src"}
  :dependencies '[[boot/core              "2.5.1"           :scope "test"]
                  [adzerk/bootlaces       "0.1.13"          :scope "test"]])

(require '[tolitius.boot-check :as check]
         '[adzerk.bootlaces :refer :all]
         '[boot.util])

(deftask test-kibit []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-kibit)))

(deftask test-yagni []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-yagni)))

(deftask test-eastwood []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-eastwood)))

(bootlaces! +version+)

(task-options!
  pom {:project     'tolitius/boot-check
       :version     +version+
       :description "check / analyze Clojure/Script code"
       :url         "https://github.com/tolitius/boot-check"
       :scm         {:url "https://github.com/tolitius/boot-check"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})

