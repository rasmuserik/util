(ns solsort.ui.icons
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.net :as net]
    [solsort.db :refer [db-url <login <first-attachment]]
    [solsort.misc :refer [starts-with <p put!close! <blob-url unique-id log]]
    [reagent.core :as reagent :refer []]
    [cljsjs.pouchdb]
    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch dispatch-sync]]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(register-sub :icons  (fn  [db _]  (reaction  (:icons @db))))
(defn icon [id] 
  (let [url (get @(subscribe [:icons]) id)]
    (when-not url
      (dispatch-sync [:load-icon id]))
    (if (and url (not= "loading" url))
      [:img.icon-img {:src url}]
      [:span "[" id "]"])))

(defonce icon-db (js/PouchDB. "icons"))
(defn all-icons! []
  (go (let [icons (map #(get % "id")
                     (-> icon-db (.allDocs) (<p) (<!) (js->clj) (get "rows")))]
      (dispatch [:all-icons icons]))))
(defn <icon-url [id] (go (<! (<blob-url (<! (<first-attachment icon-db id))))))

(register-handler :icon-loaded (fn [db [_ id icon]] (assoc-in db [:icons id] icon)))
(register-handler
  :load-icon
  (fn [db [_ id]]
    (when-not (get (:icons db) id)
      (go (dispatch [:icon-loaded id (<! (<icon-url id))])))
    (assoc-in db [:icons id] "loading")))
(register-handler :all-icons (fn [db [_ ids]] (assoc db :all-icons ids)))
(register-handler 
  :icon-cloud-sync 
  (fn [db [_ ids]] 
    (go
      (when-not (<! (<p (.sync icon-db (db-url "icons"))))
        (js/alert "Sync error")) 
      (all-icons!))
    db))
(register-sub :all-icons (fn [db] (reaction (:all-icons @db))))

