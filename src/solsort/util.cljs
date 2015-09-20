(ns ^:figwheel-always solsort.util
  (:require
    [solsort.misc :as misc]
    [solsort.net :as net]
    [solsort.ui :as ui]
    [solsort.db :as db]
    [solsort.style :as style]
    [solsort.router :as router]
    [reagent.ratom :as ratom :refer-macros [reaction]]
    [re-frame.core :refer [register-handler register-sub dispatch]]
    ))

(defn start []
  (style/load-default-style!)
  (router/start))

(if (= "complete" js/document.readyState)
  (js/setTimeout start 0)
  (js/document.addEventListener "DOMContentLoaded" start))
(js/window.addEventListener "hashchange" start)

(def host net/host)
(def log misc/log)
(def canonize-string misc/canonize-string)
(def hex-color misc/hex-color)
(def unique-id misc/unique-id)

(def app ui/app)

(def route router/route)

(def <ajax net/<ajax)

;; # log global error messages
(register-handler :error (fn [db [_ e] _] (log 'error (.-message e) e) db))
(defonce initialise
  (do (js/window.addEventListener "error" #(dispatch [:error %]))))

;; # debug
(register-sub :db (fn [db _] (reaction @db)))
#_(js/console.log (clj->js @(subscribe [:db]))) ; debug

