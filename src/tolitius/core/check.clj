(ns tolitius.core.check)

(defn coords
  ([file line column]
   (coords file line column nil nil))
  ([file line column line-end column-end]
   {:file file :line line :column column :line-end line-end :column-end column-end}))

(defn issue
  ([linter message coords]
   (issue linter nil message coords nil))
  ([linter message coords snippet]
   (issue linter nil message coords snippet))
  ([linter key message coords snippet]
   (issue linter nil key message coords snippet))
  ([linter category key message coords snippet]
   (issue linter category key message coords :normal snippet))
  ([linter category key message coords severity snippet]
   (issue linter category key message coords severity snippet nil))
  ([linter category key message coords severity snippet issue-form]
   (issue linter category key message coords severity snippet issue-form nil))
  ([linter category key message coords severity snippet issue-form hint-form]
   {:id (str (java.util.UUID/randomUUID))
    :linter-tool linter
    :category category
    :key key
    :severity severity
    :message message
    :coords coords
    :snippet snippet
    :issue-form issue-form
    :hint-form hint-form}))
