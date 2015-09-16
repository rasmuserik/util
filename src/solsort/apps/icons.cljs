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
    [solsort.db :refer [db-url]]
    [solsort.ui]
    [reagent.core :as reagent :refer []]
    [cljsjs.pouchdb]
    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch]]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(defn starts-with [s prefix] (= (.slice s 0 (.-length prefix)) prefix))
(declare icon-db)
(declare all-icons)
(declare <icon-url)
(declare <p)

(register-handler 
  :icon-loaded 
  (fn [db [_ id icon]] 
    (assoc-in db [:icons id] icon)))

(register-handler
  :load-icon
  (fn [db [_ id]]
    (when-not (get (:icons db) id)
      (go (dispatch [:icon-loaded id (<! (<icon-url id))])))
    db))

(register-handler :all-icons (fn [db [_ ids]] (assoc db :all-icons ids)))
(register-handler 
  :icon-cloud-sync 
  (fn [db [_ ids]] 
    (go
      (when-not (<! (<p (.sync icon-db (db-url "icons"))))
        (js/alert "Sync error")) 
      (all-icons))
    db))
(register-handler 
  :add-icons-dialog 
  (fn [db _]  (.click (js/document.getElementById "iconfile-input")) db))
(register-handler 
  :delete-icon? 
  (fn [db [_ id]] 
    (when (js/confirm "Delete icon?")
      (go (<! (<p (.remove icon-db (<! (<p (.get icon-db id))))))
          (all-icons)))
    db))
(register-sub :all-icons (fn [db] (reaction (:all-icons @db))))

(defonce icon-db (js/PouchDB. "icons"))
(defn put!close! [c d] (if (nil? d) (close! c) (put! c d)))
(defn <p 
  "Convert a javascript promise to a core.async channel"
  [p]
  (let [c (chan)]
    (.then p #(put!close! c %))
    (.catch p #(close! c))
    c))

(defn <first-attachment-id [db id]
  (go (let [a (<! (<p (.get db id))) 
            a (and a (aget a "_attachments"))]
        (and a (aget (js/Object.keys a) 0)))))
(defn <first-attachment [db id]
  (go (<! (<p  (.getAttachment db id (<! (<first-attachment-id db id)))))))

(defn <blob-url [blob]
  (let [reader (js/FileReader.)
        c (chan)]
    (aset reader "onloadend" #(put!close! c (aget reader "result")))
    (if blob
      (.readAsDataURL reader blob)
      (close! c))
    c))

(defn <icon-url [id] (go (<! (<blob-url (<! (<first-attachment icon-db id))))))

;; # Icon list/upload app
(defn show-icon [id]
  [:span.inline-block {:style {:margin 10 :text-align "center"} :on-click #(dispatch [:delete-icon? id])} 
   [:span {:style {:font-size "250%"}} [solsort.ui/icon id]] [:br] 
   [:span {:style {:font-size "70%"}} id]]) 

(defn all-icons []
  (go
    (let [icons (map #(get % "id")
                     (-> icon-db (.allDocs) (<p) (<!) (js->clj) (get "rows")))]
      (dispatch [:all-icons icons]))))

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
                   (re-find #"\d+" fname)
                   (re-find #"[^.]+" fname))]
          (go

            (<! (<p (.put icon-db #js{:_id id})))
            (let [doc (<! (<p (.get icon-db id)))]
              (<! (<p (.putAttachment icon-db id "icon" (aget doc "_rev") file (aget file "type")))))
            (all-icons)))))))

(route "icons" :app
       (fn []
         (go (<! (<p (.replicate.from icon-db (db-url "icons"))))
          (all-icons))
         (reaction {:type :app
                    :title "Icons"
                    :actions [{:event [:icon-cloud-sync] :icon "31173"}
                              {:event [:add-icons-dialog] :icon "89834"}  ]
                    :html
                    [:div
                     [:input.hidden {:id "iconfile-input" 
                                     :type "file" 
                                     :multiple true 
                                     :on-change upload-files}]
                     (into [:div] (map show-icon @(subscribe [:all-icons])))]})))
