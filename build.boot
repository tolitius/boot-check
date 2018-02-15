(def +version+ "0.1.8")

(set-env!
  :source-paths #{"src"}
  :dependencies '[[boot/core              "2.7.2"]
                  [adzerk/bootlaces       "0.1.13"          :scope "test"]
                  [hiccup                 "1.0.5"]
                  [pandeiro/boot-http     "0.8.3"]])


(require '[tolitius.boot-check :as check]
         '[adzerk.bootlaces :refer :all]
         '[boot.util]
         '[tolitius.reporter.html :refer :all]
         '[pandeiro.boot-http :refer :all])

(deftask test-kibit []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-kibit :options {:gen-report true})))

(deftask test-yagni []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-yagni :options {:gen-report true
                                :entry-points ["test.with-yagni/-main"
                                               "test.with-yagni/func-the-second"
                                               42]})))

(deftask test-eastwood []
  (set-env! :source-paths #{"src" "test"} :boot-check-reporter :html)
  (comp
    (check/with-eastwood :options {:gen-report true :exclude-linters [:unused-ret-vals]})))

(deftask test-eastwood-no-report []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-eastwood :options {:exclude-linters [:unused-ret-vals]})))

(deftask test-eastwood-and-throw []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-eastwood :options {:gen-report true :exclude-linters [:unused-ret-vals]})
    (check/throw-on-errors)))

(deftask test-bikeshed []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-bikeshed :options {:check? #{:long-lines :trailing-whitespace :var-redefs :bad-methods :name-collisions}
                                   :max-line-length 42
                                   :gen-report true})))

(deftask check-all []
  (comp
    (test-kibit)
    (test-yagni)
    (test-eastwood)
    (test-bikeshed)))

(deftask check-all-and-throw []
  (comp
    (test-kibit)
    (test-yagni)
    (test-eastwood)
    (test-bikeshed)
    (check/throw-on-errors)))

(deftask check-all-serve []
  (comp
    (serve)
    (test-kibit)
    (test-yagni)
    (test-eastwood)
    (test-bikeshed)
    (wait)))

(deftask check-all-serve-watch []
  (comp
    (serve)
    (watch)
    (test-kibit)
    (test-yagni)
    (test-eastwood)
    (test-bikeshed)))

(bootlaces! +version+)

(task-options!
  pom {:project     'tolitius/boot-check
       :version     +version+
       :description "check / analyze Clojure/Script code"
       :url         "https://github.com/tolitius/boot-check"
       :scm         {:url "https://github.com/tolitius/boot-check"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})
