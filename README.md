# boot-check

[Boot](https://github.com/boot-clj/boot) tasks to check, analyze and inspect Clojure/Script code.

It relies on universe tested [kibit](https://github.com/jonase/kibit), 
[eastwood](https://github.com/jonase/eastwood), [yagni](https://github.com/venantius/yagni), [bikeshed](https://github.com/dakrone/lein-bikeshed) and other titans.

[![Clojars Project](http://clojars.org/tolitius/boot-check/latest-version.svg)](http://clojars.org/tolitius/boot-check)

- [Why](#why)
- [Kibit](#kibit)
  - [From Command Line](#from-command-line)
  - [From within "build.boot"](#from-within-buildboot)
  - [Help](#help)
- [Yagni](#yagni)
  - [From Command Line](#from-command-line-1)
  - [From within "build.boot"](#from-within-buildboot-1)
  - [Help](#help-1)
  - [Yagni entry points](#yagni-entry-points)
- [Eastwood](#eastwood)
  - [From Command Line](#from-command-line-2)
  - [From within "build.boot"](#from-within-buildboot-2)
  - [Help](#help-2)
- [Bikeshed](#bikeshed)
  - [From Command Line](#from-command-line-2)
  - [From within "build.boot"](#from-within-buildboot-2)
  - [Help](#help-2)
  - [Bikeshed options](#bikeshed-options)
- [Demo](#demo)
- [License](#license)

## Why

To be able to reach out to multiple code analyzers as well as compose them as [Boot tasks](https://github.com/boot-clj/boot/wiki/Tasks):

```clojure
(require '[tolitius.boot-check :as check])
```

```clojure
(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-yagni)
    (check/with-eastwood)
    (check/with-kibit)
    (check/with-bikeshed)))
```

You can choose the tools (tasks) that apply, i.e. use one or several, and `boot-check` will do the rest: integration with analyzers, dependencies, reports, etc..

All these tasks will run inside [Boot pods](https://github.com/boot-clj/boot/wiki/Pods).

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

WARN: kibit found some problems:

{:problems #{{:expr (if 42 42 nil), :line 4, :column 3, :alt (when 42 42)} 
             {:expr (into [] 42), :line 7, :column 3, :alt (vec 42)}}}
```

### From within "build.boot"

To use `boot-check` tasks within `build.boot` is easy:

```clojure
(require '[tolitius.boot-check :as check])

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-kibit)))
```

### Help

```shell
$ boot check/with-kibit -h
Static code analyzer for Clojure, ClojureScript, cljx and other Clojure variants.

This task will run all the kibit checks within a pod.

At the moment it takes no arguments, but behold..! it will. (files, rules, reporters, etc..)

Options:
  -h, --help  Print this help info.
```

## Yagni

[yagni](https://github.com/venantius/yagni) is a static code analyzer that helps you find unused code in your applications and libraries.

### From Command Line

To check your code directly from shell:

```shell
$ boot check/with-yagni
latest report from yagni.... [You Rock!]
```

if Yagni finds [unused code](test/test/with_yagni.clj) it will gladly report the news:

```shell
WARN: could not find any references to the following:

tolitius.yagni/check
test.with-yagni/func-the-second
test.with-yagni/other-func
tolitius.yagni/report
test.with-kibit/vec-vs-into
test.with-yagni/-main

WARN: the following have references to them, but their parents do not:

tolitius.yagni/yagni-deps
tolitius.yagni/pp
test.with-kibit/when-vs-if
test.with-yagni/func
test.with-yagni/notafunc
```

### From within "build.boot"

To use `boot-check` tasks within `build.boot` is easy:

```clojure
(require '[tolitius.boot-check :as check])

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-yagni)))
```

### Help

```shell
$ boot check/with-yagni -h
Static code analyzer for Clojure that helps you find unused code in your applications and libraries.

This task will run all the yagni checks within a pod.

Options:
  -h, --help             Print this help info.
  -o, --options OPTIONS  OPTIONS sets yagni options EDN map.
```

#### Yagni entry points

Yagni works by searching your codebase from an initial set of entrypoints. As libraries, multi-main programs, and certain other types of projects either tend to have no `:main` or many entrypoint methods, you can instead, optionally, enumerate a `list of entrypoints` for your project in options:

```clojure
(check/with-yagni :options {:entry-points ["test.with-yagni/-main"
                                           "test.with-yagni/func-the-second"
                                           42]})))
```

check out the [example](https://github.com/tolitius/boot-check/blob/master/build.boot#L21-L23) in the `boot.build` of this project.

## Eastwood

[eastwood](https://github.com/jonase/eastwood) is a Clojure [lint](http://en.wikipedia.org/wiki/Lint_%28software%29) tool that uses the [tools.analyzer](https://github.com/clojure/tools.analyzer) and [tools.analyzer.jvm](https://github.com/clojure/tools.analyzer.jvm) libraries to inspect namespaces and report possible problems.

### From Command Line

To check your code directly from shell:

```shell
$ boot check/with-eastwood
latest report from eastwood.... [You Rock!]
```
if eastwood finds [problems](test/test/with_eastwood.clj) it will gladly report the news:

```shell
== Linting test.with-kibit ==
... /test/with_kibit.clj:4:3: constant-test: Test expression is always logical true or always logical false: 42 in form (if 42 42 nil)

== Linting test.with-eastwood ==
... /test/with_eastwood.clj:5:8: def-in-def: There is a def of a nested inside def nested-def

== Warnings: 2 (not including reflection warnings)  Exceptions thrown: 0

WARN: eastwood found some problems ^^^
```

### From within "build.boot"

To use `boot-check` tasks within `build.boot` is easy:

```clojure
(require '[tolitius.boot-check :as check])

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-eastwood)))
```

### Help

```shell
$ boot check/with-eastwood -h
Clojure lint tool that uses the tools.analyzer and tools.analyzer.jvm libraries to inspect namespaces and report possible problems

This task will run all the eastwood checks within a pod.

At the moment it takes no arguments, but behold..! it will. (linters, namespaces, etc.)

Options:
  -h, --help  Print this help info.
```

## Bikeshed

[bikeshed](https://github.com/dakrone/lein-bikeshed) is a Clojure "checkstyle/pmd" tool that designed to tell you your code is bad, and that you should feel bad.

### From Command Line

To check your code directly from shell:

```shell
$ boot check/with-bikeshed
latest report from bikeshed.... [You Rock!]
```
if bikeshed finds problems it will gladly report the news:

```shell
Checking for lines longer than 80 characters.
Badly formatted files:
../tolitius/boot_check.clj:8:            [boot.core :as core :refer [deftask user-files tmp-file set-env! get-env]]
../tolitius/boot_check.clj:25:  "Static code analyzer for Clojure, ClojureScript, cljx and other Clojure variants.
../tolitius/boot_check.clj:29:  At the moment it takes no arguments, but behold..! it will. (files, rules, reporters, etc..)"
../tolitius/boot_check.clj:30:  ;; [f files FILE #{sym} "the set of files to check."]      ;; TODO: convert these to "tmp-dir/file"

Checking for lines with trailing whitespace.
Badly formatted files:
../tolitius/boot/helper.clj:6:  (mapv #(.getAbsolutePath %)
../tolitius/checker/bikeshed.clj:7:  '[[lein-bikeshed "0.2.0" :exclusions [org.clojure/tools.cli
../tolitius/checker/yagni.clj:33:      (let [graph# (binding [*ns* (the-ns *ns*)]
../tolitius/boot/helper.clj:6:  (mapv #(.getAbsolutePath %)
../tolitius/checker/bikeshed.clj:7:  '[[lein-bikeshed "0.2.0" :exclusions [org.clojure/tools.cli
../tolitius/checker/yagni.clj:33:      (let [graph# (binding [*ns* (the-ns *ns*)]

Checking for files ending in blank lines.
No files found.

Checking for redefined var roots in source directories.
No with-redefs found.

Checking whether you keep up with your docstrings.
9/50 [18.00%] functions have docstrings.
Use -v to list functions without docstrings

WARN: bikeshed found some problems ^^^
```

### From within "build.boot"

To use `boot-check` tasks within `build.boot` is easy:

```clojure
(require '[tolitius.boot-check :as check])

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-bikeshed)))
```

### Help

```shell
$ boot check/with-bikeshed -h

This task is backed by 'lein-bikeshed' which is designed to tell you your code is bad, and that you should feel bad

This task will run all the bikeshed checks within a pod.

At the moment it takes no arguments, but behold..! it will. ('-m, --max-line-length', etc.)

Options:
  -h, --help             Print this help info.
  -o, --options OPTIONS  OPTIONS sets bikeshed options EDN map.
```

### Bikeshed Options

Bikeshed takes a couple of options:

```clojure
(check/with-bikeshed :options {:verbose true
                               :max-line-length 42})
```

or

```
$ boot check/with-bikeshed -o '{:max-line-length 4}'
```

check out the [example](https://github.com/tolitius/boot-check/blob/master/build.boot#L34-L35) in the boot.build of this project.

## Demo

Here is a boot check [demo project](https://github.com/tolitius/check-boot-check) which can be cloned and played with.

## License

Copyright © 2015 toliitus

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
