(ns solsort.lib.icon
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.net :as net]
    [solsort.style :refer [add-default-style]]
    [solsort.db :refer [db-url <login <first-attachment]]
    [solsort.misc :refer [starts-with <p put!close! <blob-url unique-id log <blob-text]]
    [reagent.core :as reagent :refer []]
    [cljsjs.pouchdb]
    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch dispatch-sync]]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(def icon-size "2em")
(def icon-margin ".25em")
(add-default-style 
  {
   :.icon-container  {:vertical-align "middle"
                      :padding icon-margin 
                      :display "inline-block"
                      :height icon-size
                      ;:box-shadow "0 0 2px #000"
                      
                      }
   :.icon-container2 {:width icon-size
                      :height icon-size
                      ;:box-shadow "0 0 2px #000"
                      :display "inline-block"
                      :vertical-align "top"
                      :padding 0
                      :overflow "hidden" }
   :.icon-img {:width icon-size :margin 0 :vertical-align "top"}})

(register-sub :icons  (fn  [db _]  (reaction  (:icons @db))))
(register-sub 
  :icon-authors
  (fn  [db _]  
    (reaction  
      (seq
        (:icon-authors @db)))))
(defn icon [id] 
  (let [url (get @(subscribe [:icons]) id)]
    (when-not url
      (dispatch-sync [:load-icon id]))
    [:span.icon-container
     [:span.icon-container2
      [:img.icon-img 
       {:src (if (and url (not= "loading" url))
               url
               (db-url (str "icons/" id "/icon") ))}]]]))

(defonce icon-db (js/PouchDB. "icons"))
(defn all-icons! []
  (go (let [icons (map #(get % "id")
                       (-> icon-db (.allDocs) (<p) (<!) (js->clj) (get "rows")))]
        (dispatch [:all-icons icons]))))
(defn <icon-url [id] 
  (go
    (let [blob (<! (<first-attachment icon-db id))]
      (cond 
        (starts-with id "emojione-")
        (dispatch-sync [:icon-author "Emoji One"])
        (starts-with id "noun-")
        (when 
          blob 
        (go (let [author (second (re-find #"Created by ([^<]*)" (<! (<blob-text blob))))]
                (dispatch-sync [:icon-author "the Noun Project"])
              (if author
                (dispatch-sync [:icon-author author])
                (dispatch-sync [:icon-author "Public Domain"])))))
        :else (dispatch-sync [:icon-author "solsort.com"])
        )
      (<! (<blob-url blob)))))

(register-handler 
  :icon-loaded 
  (fn [db [_ id icon]] 
    (assoc-in db [:icons id] icon)))
(register-handler
  :load-icon
  (fn [db [_ id]]
    (when-not (get (:icons db) id)
      (go (dispatch [:icon-loaded id (<! (<icon-url id))])))
    (assoc-in db [:icons id] "loading")))
(register-handler :all-icons (fn [db [_ ids]] (assoc db :all-icons ids)))
(register-handler 
  :icon-author 
  (fn [db [_ author]] 
    (assoc 
      db :icon-authors 
      (conj (get db :icon-authors #{}) author))))

(register-handler 
  :icon-cloud-sync 
  (fn [db [_ ids]] 
    (go
      (when-not (<! (<p (.sync icon-db (db-url "icons"))))
        (js/alert "Sync error")) 
      (all-icons!))
    db))
(register-sub :all-icons (fn [db] (reaction (:all-icons @db))))

; TODO, load icons on demand, instead of sync all
(go (<! (<p (.replicate.from icon-db (db-url "icons")))) 
    (all-icons!)) 
