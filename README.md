# boot-check

[Boot](https://github.com/boot-clj/boot) tasks to check, analyze and inspect Clojure/Script code.

It relies on universe tested [kibit](https://github.com/jonase/kibit), 
[eastwood](https://github.com/jonase/eastwood), [yagni](https://github.com/venantius/yagni) and other titans.

[![Clojars Project](http://clojars.org/tolitius/boot-check/latest-version.svg)](http://clojars.org/tolitius/boot-check)

###### _Source Code Static Analyzers_

## Kibit

[kibit](https://github.com/jonase/kibit) is a static code analyzer for Clojure, ClojureScript, cljx and other Clojure variants.

### From Command Line

To check your code directly from shell:

```bash
$ boot check/with-kibit
latest report from kibit.... [You Rock!]
```

In case there are [problems](test/test/with_kibit.clj):

```clojure
(defn when-vs-if []
  (if 42 42 nil))

(defn vec-vs-into []
  (into [] 42))
```

kibit will show suggestions:

```clojure
$ boot check/with-kibit
At ../.boot/cache/tmp/../fun/boot-check/yeg/-grrwi1/test/with_kibit.clj:4:
Consider using:
  (when 42 42)
instead of:
  (if 42 42 nil)

At ../.boot/cache/tmp/../fun/boot-check/yeg/-grrwi1/test/with_kibit.clj:7:
Consider using:
  (vec 42)
instead of:
  (into [] 42)

clojure.lang.ExceptionInfo: kibit found some problems:  {:problems #{{:expr (if 42 42 nil), :line 4, :column 3, :alt (when 42 42)}
                                                                     {:expr (into [] 42), :line 7, :column 3, :alt (vec 42)}}}
```

### From Within "build.boot"

To use boot-check tasks within `build.boot` is easy:

```clojure
(require '[tolitius.boot-check :as check])

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-kibit)))
```

## Yagni

[yagni](https://github.com/venantius/yagni) is a static code analyzer that helps you find unused code in your applications and libraries

### From Command Line

To check your code directly from shell:

```clojure
$ boot -v check/with-yagni
latest report from yagni.... [You Rock!]
```

if Yagni finds unused code it will gladly report the news:

```clojure
WARN: could not find any references to the following:

tolitius.yagni/check
test.with-yagni/func-the-second
test.with-yagni/other-func
tolitius.yagni/report
test.with-kibit/vec-vs-into
test.with-yagni/-main

WARN: The following have references to them, but their parents do not:

tolitius.yagni/yagni-deps
tolitius.yagni/pp
test.with-kibit/when-vs-if
test.with-yagni/func
test.with-yagni/notafunc
```

### From Within "build.boot"

To use boot-check tasks within `build.boot` is easy:

```clojure
(require '[tolitius.boot-check :as check])

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-yagni)))
```

## Help

### kibit

```shell
$ boot check/with-kibit -h
Static code analyzer for Clojure, ClojureScript, cljx and other Clojure variants.

This task will run all the kibit checks within a pod.

At the moment it takes no arguments, but behold..! it will. (files, rules, reporters, etc..)

Options:
  -h, --help  Print this help info.
```

### yagni

```shell
$ boot check/with-yagni -h
Static code analyzer for Clojure that helps you find unused code in your applications and libraries.

This task will run all the yagni checks within a pod.

At the moment it takes no arguments, but behold..! it will.

Options:
  -h, --help  Print this help info.
```

## License

Copyright © 2015 toliitus

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
