# osacljs

Use Replumb in OSX Javascript for Automation (JXA)

This is a attempt to port mfikes [elbow](https://github.com/mfikes/elbow) to JXA.

# Usage

1. `lein cljsbuild once`
2. `osascript out/main.js <src-paths>`

where `src-paths` looks like `src1:/bar/src2:/foo/src3`

# Example

```
$ osascript out/main.js
cljs.user=> 3
3
cljs.user=> (+ 3 2)
5
cljs.user=> (ns foo.bar)
nil
foo.bar=> ::a
:foo.bar/a
```


