(ns solsort.toolbox.macros)

(defmacro <? [expr]
  `(solsort.toolbox.misc/throw-error (cljs.core.async/<! ~expr)))
