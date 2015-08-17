;; # About
;; # Dependency declarations
(ns ^:figwheel-always lemon.core
  (:require-macros
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(enable-console-print!)

;; # App-state
(defonce app-state
  (reagent/atom
    {:path ["index"]
     }))

;; # Main
(defn front-page []
  (print 'hi)
  [:div
   [:h1 "hello"]])

(defn main []
  (case (first (:path @app-state))
    "index" (front-page))
  )

(reagent/render-component main (.getElementById js/document "app"))

(defn on-js-reload [])
