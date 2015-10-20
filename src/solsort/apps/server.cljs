(ns solsort.apps.server
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [route log unique-id]]
    [solsort.misc :refer [<n]]
    [solsort.ui :refer [app input]]
    [reagent.core :as reagent :refer []]
    [re-frame.core :as re-frame :refer [subscribe]]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))
(when (and false
        (exists? window.require)
        (exists? (js/require "levelup")))
  (go
    (defonce db ((js/require "levelup") "/solsort/hello.leveldb"))
    (.put 
      db "is it" "working" 
      (fn [err]
        (.get db "is it"
              (fn [err result]
                (log 'levelup result err)
                ))))))

(defn log-elem []
  [:div (map (fn [e] [:div {:key (unique-id)} (.slice (str e) 1 -1)]) 
             (reverse @(subscribe [:log])))])
(route 
  "server" 
  (app
    {:title "solsort server"
     ;:navigate-back {:event ['home] :title "Home" :icon "emojione-lemon"}
     :actions [;{:event [:log "pressed hello"] :icon "emojione-airplane"}
               ]
     ;:views [;{:event ['view-left] :icon "left"}
     ;        ]
     :html
     [:div
      [log-elem]]}))
