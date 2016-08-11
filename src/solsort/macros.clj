(ns solsort.macros)

(defmacro <? [expr]
  `(solsort.util/throw-error (cljs.core.async/<! ~expr)))
