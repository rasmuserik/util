(ns solsort.toolbox.macros)

(defmacro <? [expr]
  `(solsort.toolbox.misc/throw-error (cljs.core.async/<! ~expr)))

(defmacro except [expr res]
  `(try ~expr
        (catch :default e
          ~res)))
