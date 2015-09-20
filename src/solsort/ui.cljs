(ns solsort.ui
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [clojure.string :as string :refer  [split]]
    [re-frame.core :as re-frame :refer [register-sub subscribe register-handler dispatch dispatch-sync]]
    [solsort.misc :as misc :refer [function? chan? unique-id unatom]]
    [solsort.net :as net]
    [solsort.lib.icon]
    [solsort.lib.app]
    [solsort.style :refer [clj->css]]
    [reagent.core :as reagent :refer  []]))

(defonce initialise
  (do
    (js/React.initializeTouchEvents true)
    (js/window.addEventListener "resize" #(dispatch [:update-viewport]))))

(register-sub :view-dimensions
              (fn  [db _]  (reaction
                             [(get-in @db  [:viewport :width])
                              (get-in @db  [:viewport :height])])))
(register-sub :width  (fn  [db _]  (reaction  (get-in @db  [:viewport :width]))))
(register-sub :height  (fn  [db _]  (reaction  (get-in @db  [:viewport :height]))))


(register-handler
  :update-viewport
  (fn [db _ _]
    (-> db (assoc-in [:viewport :width] js/window.innerWidth)
        (assoc-in [:viewport :height] js/window.innerHeight))))

(def icon solsort.lib.icon/icon)
(def app solsort.lib.app/app)
