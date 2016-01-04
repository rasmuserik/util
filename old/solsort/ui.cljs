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
    (dispatch [:update-viewport])
    (js/window.addEventListener "load" #(dispatch [:update-viewport]))  
    (js/window.addEventListener "resize" #(dispatch [:update-viewport]))))

(register-sub :view-dimensions
              (fn  [db _]  (reaction
                             [(get-in @db  [:viewport :width])
                              (get-in @db  [:viewport :height])])))
(register-sub :width  (fn  [db _]  (reaction  (get-in @db  [:viewport :width]))))
(register-sub :height  (fn  [db _]  (reaction  (get-in @db  [:viewport :height]))))

(register-sub :view (fn  [db _]  (reaction  (get-in @db  [:view]))))
(register-handler :view (fn  [db [_ view]]  (assoc db :view view)))

(register-handler
  :update-viewport
  (fn [db _]
    (-> db (assoc-in [:viewport :width] js/window.innerWidth)
        (assoc-in [:viewport :height] js/window.innerHeight))))

(def default-shadow "1px 1px 3px rgba(0,0,0,0.4)")
(def icon solsort.lib.icon/icon)
(def app solsort.lib.app/app)
(register-sub :form-value (fn [db [_ s]] (reaction (get-in @db [:form s]))))
(register-handler :form-value (fn [db [_ s v]] 
                                (assoc-in db [:form s] v)))
(defn input [& {:keys [type data name value class placeholder style id rows]
                :or {type "text"
                     name "missing-name" }}]
  (let [class  (str "solsort-input solsort-input-" type " " class) 
        parent-id (unique-id)
        value-key (case type
                    ("checkbox") "checked"
                    "value")
        ]
    [:span {:id unique-id}
     [(if (= type "textarea") :textarea :input) 
     {:type type 
             :id (str name "-input")
             (keyword value-key) @(subscribe [:form-value name])
             :placeholder placeholder
             :on-focus #(dispatch [:show-bars false])
             :on-blur #(go (<! (timeout 300)) (dispatch [:show-bars true]))
             :rows rows
             :style style
             :class class
             :on-change 
             (fn [e] 
               (dispatch-sync [:form-value name (-> e .-target (aget value-key))])) }]]))

(def add-style add-default-style)

(add-default-style 
  {:button
   {:background "rgba(255,255,255,0.75);" 
    :box-shadow default-shadow
    :border :none
    :padding 5 }
   :.solsort-input-checkbox 
   {:display :inline-block
    :background "red"
    :transform "scale(1.5)"
    :margin "10px"
    }
   ".solsort-input:focus" 
   {:outline "0"
    :border "1px solid #48f"
    :box-shadow "0px 0px 7px rgba(50,100,255,0.5)"
    
    }
   :.solsort-input 
   {:background "#fff"
    :border-radius 4
    :border "1px solid #ccc"
    :padding "0.5em"
    ;:margin ".5em"
    ;:box-shadow (str default-shadow " inset")

    ;:box-shadow "1px 1px 5px rgba(0,0,0,0.7) inset"
    :vertical-align "baseline"
   }})
