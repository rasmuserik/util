(ns ^:figwheel-always solsort.main
  (:require
    [reagent.core :as reagent]
    [solsort.util]
    ))
(defn figwheel-load []
  (js/console.log 'figwheel-load))
