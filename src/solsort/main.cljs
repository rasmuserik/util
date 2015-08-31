(ns ^:figwheel-always solsort.main
  (:require
    [solsort.bib]
    [solsort.core :refer [dispatch-route]]
    [solsort.experiments]
    [solsort.index]
    [solsort.lemon]
    [solsort.rasmuserik]))

(defn on-js-reload [] (dispatch-route))
(js/setTimeout 0 dispatch-route)
