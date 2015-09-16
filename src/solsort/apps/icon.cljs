(ns solsort.apps.icon
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

(declare icon-db)
(declare <icon-url)

(register-handler 
  :icon-loaded 
  (fn [db [_ id icon]] 
    (log 'icon-loaded id icon)
    (assoc-in db [:icons id] (or icon "TODO: missing icon icon"))))

(register-handler
  :load-icon
  (fn [db [_ id]]
    (when-not (get (:icons db) id)
      (go (dispatch [:icon-loaded id (<! (<icon-url id))])))
    (assoc-in db [:icons id] "TODO: missing icon icon")))

(register-handler :all-icons (fn [db [_ ids]] (assoc db :all-icons ids)))
(register-handler :add-icons-dialog (fn [db _]  (.click (js/document.getElementById "iconfile-input")) db))
(register-sub :all-icons (fn [db] (reaction (:all-icons @db))))
;; # PouchDB shorthands
(defn put!close! [c d] (if (nil? d) (close! c) (put! c d)))
(defn <p 
  "Convert a javascript promise to a core.async channel"
  [p]
  (let [c (chan)]
    (.then p #(put!close! c %))
    (.catch p #(close! c))
    c))

(defn <first-attachment-id [db id]
  (go (let [a (aget (<! (<p (.get db id))) "_attachments")]
        (and a (aget (js/Object.keys a) 0)))))
(defn <first-attachment [db id]
  (go (<! (<p  (.getAttachment db id (<! (<first-attachment-id db id)))))))

(defn <blob-url [blob]
  (let [reader (js/FileReader.)
        c (chan)]
    (aset reader "onloadend" #(put!close! c (aget reader "result")))
    (.readAsDataURL reader blob)
    c))

(defn <icon-url [id] (go (<! (<blob-url (<! (<first-attachment icon-db id))))))


;; #experiments
(defonce icon-db (js/PouchDB. "icons"))
(go
  (js/console.log "firstattach" (<! (<first-attachment icon-db "solsort")))
  (log 'blob (<! (<blob-url (<!  (<first-attachment icon-db "solsort")))))
  (let [[u p] @(subscribe [:db-login])]
    (when (= u "daemon")
            (.sync icon-db (db-url "icons"))
      ;      (log "solsort" (<! (<p (.get icon-db "solsort"))))  
      ;      (js/console.log "attach" (<! (<p (.getAttachment icon-db "solsort" "solsort.svg"))))

      )  ) )

(js/console.log (.allDocs icon-db))

(defn show-icon [id]
  [:span.icon-sample [solsort.ui/icon id] [:br]  id " "]
  )
;; # Sample app
(route "icon" :app
       (fn []
         (go
           (let [icons (map #(get % "id")
                            (-> icon-db (.allDocs) (<p) (<!) (js->clj) (get "rows")))]
             (dispatch [:all-icons icons])
           (log "all-docs" icons)
           
           ))
         (reaction {:type :app
                    :title "solsort"
                    :navigate-back {:event ['home] :title "Home" :icon "solsort"}
                    :actions [ {:event [:log "pressed hello"] :icon "hello"}
                              {:event ['paste] :icon "paste"} ]
                    :views [ {:event ['view-left] :icon "left"}
                            {:event [:add-icons-dialog] :icon "89834"} ]
                    :html
                    [:div
                     [:input.hidden {:id "iconfile-input" 
                                     :type "file" 
                                     :multiple true 
                                     :on-change (fn [a] (js/console.log "file-change" a))}]
                     (into [:div]
                           (map show-icon @(subscribe [:all-icons])))
                     (map (fn [e] [:div {:key (unique-id)} (.slice (str e) 1 -1)]) (reverse @(subscribe [:log])))
                     (str (range 1000))]})))
