(ns solsort.apps.icons
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [route log unique-id]]
    [solsort.net :as net]
    [solsort.db :refer [db-url <login]]
    [solsort.ui :refer [app icon]]
    [solsort.ui.icons :refer [icon-db all-icons!]]
    [solsort.misc :refer [starts-with <p put!close!]]
    [reagent.core :as reagent :refer []]
    [cljsjs.pouchdb]
    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch]]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(register-handler 
  :add-icons-dialog 
  (fn [db _]  (.click (js/document.getElementById "iconfile-input")) db))

(register-handler 
  :delete-icon? 
  (fn [db [_ id]] 
    (when (js/confirm "Delete icon?")
      (go (<! (<p (.remove icon-db (<! (<p (.get icon-db id))))))
          (all-icons!)))
    db))
(register-sub :icon-start (fn [db] (reaction (get @db :icon-start 0))))
(register-handler 
  :icon-start-inc 
  (fn [db [_ cnt]] 
    (assoc 
      (if (< (js/Math.random) .95)
        db
        (assoc db :icons {}))
      :icon-start
      (let [prev (get db :icon-start 0)
            start (+ prev cnt)
            start (max 0 start)]
        (if (< start (count (:all-icons db))) start prev)))))

(defn show-icon [id]
  [:span.inline-block {:style {:margin 10 :text-align "center"} :on-click #(dispatch [:delete-icon? id])} 
   [:span {:style {:font-size "250%"}} [icon id]] [:br] 
   [:span {:style {:font-size "70%"}} id]]) 


(defn upload-files []
  (let [elem (js/document.getElementById "iconfile-input")
        files (aget elem "files")
        files (loop [fs [] i 0]
                (if (< i (.-length files))
                  (recur (conj fs (aget files i)) (inc i))
                  fs))]
    (doall 
      (for [file files]
        (let [fname (aget file "name")
              id (if (starts-with fname "noun_")
                   (str "noun-" (re-find #"\d+" fname))
                   (re-find #"[^.]+" fname))]
          (go (<! (<p (.put icon-db #js{:_id id})))
              (let [doc (<! (<p (.get icon-db id)))]
                (<! (<p (.putAttachment icon-db id "icon" 
                                        (aget doc "_rev") file (aget file "type")))))
              (all-icons!)))))))

(def icon-step 24)
(defn show-all-icons []
  (into [:div] (map show-icon (take icon-step(drop @(subscribe [:icon-start]) 
                                                   @(subscribe [:all-icons]))))))
(route 
  "icons"
  (fn [o]
    (when (:reactive o)
      (<login (get o "user") (get o "password"))
      (all-icons!)
      (go (<! (<p (.replicate.from icon-db (db-url "icons")))) 
          (all-icons!)))
    (app {:type :app
          :title (reaction (str "Icons " 
                                (inc (/ @(subscribe [:icon-start]) icon-step)) "/"
                                (js/Math.ceil (/ (count @(subscribe [:all-icons])) icon-step))
                                ))
          :views[
                 {:event [:icon-start-inc (- icon-step)] :icon "noun-26915"}
                 {:event [:icon-start-inc icon-step] :icon "noun-26914"}
                 {:event [:icon-cloud-sync] :icon "noun-31173"}
                 {:event [:add-icons-dialog] :icon "noun-89834"}]
          :html
          [:div
           [:input.hidden {:id "iconfile-input" 
                           :type "file" 
                           :multiple true 
                           :on-change upload-files}]
           [show-all-icons]
           ]} o)))
