(ns ^:figwheel-always solsort.main
  (:require
    [solsort.bib]
    [solsort.core :refer [dispatch-route log]]
    [solsort.experiments]
    [solsort.index]
    [solsort.lemon]
    [solsort.rasmuserik]))

(defn on-js-reload [] (dispatch-route))
(js/setTimeout dispatch-route 0)
(log 'here-in-main)
