## 0.1.7
###### Wed Jan 24 18:08:53 2018 -0500

* updating bikeshed to 0.5.1 ([#16](https://github.com/tolitius/boot-check/issues/16))
  - bikeshed options changed, it now requires `:check?` option to include possible checks:
```clojure
(check/with-bikeshed :options {:check? #{:long-lines}
                               :verbose true
                               :max-line-length 42})
```
or
```bash
$ boot check/with-bikeshed -o '{:check? #{:long-lines :trailing-whitespace :var-redefs :bad-methods :name-collisions}}'
```
