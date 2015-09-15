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
    [reagent.core :as reagent :refer []]
    [cljsjs.pouchdb]
    [re-frame.core :as re-frame :refer [subscribe register-handler]]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(register-handler 
  :icon-loaded
  (fn [db [_ i]] 
       (assoc db :icons
         (assoc (or (:icons db) {}) (:id i) i))))

;; # PouchDB shorthands
(defn all-docs [db]
  (let [c (chan)]
    (.allDocs db (fn [err data] 
                   (if (or err (= nil data))
                     (close! c)
                     (put! c data))))
    c))
(defn pouch-get [db id]
  (let [c (chan)]
    (.get db 
          id
          (fn [err data] 
                   (if (or err (= nil data))
                     (close! c)
                     (put! c data))))
    c))
(defn pouch-get-attachment [db id f]
  (let [c (chan)]
    (.getAttachment db 
          id f
          (fn [err data] 
                   (if (or err (= nil data))
                     (close! c)
                     (put! c data))))
    c))
;; #experiments
(defonce icon-db (js/PouchDB. "icons"))
(go
  (let [[u p] @(subscribe [:db-login])]
  (when (= u "daemon")
  (.sync icon-db (db-url "icons"))
  (log "all-docs" (js->clj (<! (all-docs icon-db))))) 
  (log "solsort" (<! (pouch-get icon-db "solsort")))  
  (js/console.log "attach" (<! (pouch-get-attachment icon-db "solsort" "solsort.svg")))  
  ) )

(js/console.log (.allDocs icon-db))

;; # Sample app
(route "icon" :app
  (fn []
    (reaction {:type :app
           :title "solsort"
           :navigate-back {:event ['home] :title "Home" :icon "home"}
           :actions [ {:event [:log "pressed hello"] :icon "hello"}
                     {:event ['paste] :icon "paste"} ]
           :views [ {:event ['view-left] :icon "left"}
                   {:event ['view-right] :icon "right"} ]
           :html
           [:div
            (map (fn [e] [:div {:key (unique-id)} (.slice (str e) 1 -1)]) (reverse @(subscribe [:log])))
            (str (range 1000))]})))
