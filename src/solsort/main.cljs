;; # Main code
;;
;; This just loads all components and apps, and dispatches the route based on the app
;;
(ns ^:figwheel-always solsort.main
  (:require
    [reagent.core :as reagent]
    [solsort.apps.bib]
    [solsort.apps.experiments]
    [solsort.apps.index]
    [solsort.apps.icons]
    [solsort.apps.lemon]
    [solsort.apps.btrie]
    [solsort.apps.server]
    [solsort.apps.rasmuserik]
    [solsort.apps.install-sites]
    [solsort.apps.hack4dk]
    [solsort.util]
    ))
