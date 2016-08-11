(ns solsort.macros)

(defmacro <? [expr]
  `(solsort.misc/throw-error (cljs.core.async/<! ~expr)))
