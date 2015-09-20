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
    [solsort.misc :as misc :refer [function? chan? unique-id unatom log]]
    [solsort.net :as net]
    [solsort.lib.icon]
    [solsort.lib.app]
    [solsort.style :refer [clj->css add-default-style]]
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
  (fn [db _]
    (-> db (assoc-in [:viewport :width] js/window.innerWidth)
        (assoc-in [:viewport :height] js/window.innerHeight))))

(def icon solsort.lib.icon/icon)
(def app solsort.lib.app/app)
(register-sub :form-value (fn [db s] (reaction (get-in @db [:form name]))))
(register-handler :form-value (fn [db [_ s v]] (assoc-in db [:form name] v)))
(defn input [& {:keys [type data name value class]
                :or {type "text"
                     name "missing-name" }}]
  (let [class  (str "solsort-input solsort-input-" type " " class) ]
  [:input {:type type 
           :value (or @(subscribe [:form-value name]))
           :class class
           :on-change #(dispatch-sync [:form-value name (-> % .-target .-value)]) }]))

(add-default-style 
  {:.solsort-input 
   {:border "none"
    :padding "0.5em"
    :margin 0
    :box-shadow "1px 1px 5px rgba(0,0,0,0.7) inset"
    :border-radius "2px"
    :vertical-align "baseline"
    :text-color "red"}})
