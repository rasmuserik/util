(ns solsort.apps.btrie
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [log <ajax host route]]
    [solsort.misc :refer [<seq<!]]
    [solsort.db :refer [db-url]]
    [clojure.string :as string]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

; - node
; - path
;   - node list
;   - 
; api
;
; sorted-list:
; - insert (bin-string)
; - lookup (prefix -> nil | iterator)

(defprotocol node
  (insert [bin-str prefix-pos])
  (lookup [bin-str]))

(defn create-path-node [s1 s2 prefix-start]
  )
(deftype leaf-node [value])
(deftype path-node [value prefix-start prefix-end])
(deftype branch-node [value])

(defn half-octet [s n]
  (let [byte-value (aget s (bit-shift-right n 1))]
  (if (bit-and 1 n) 
    (bit-and byte-value 0xf) 
    (bit-shift-right byte-value 4))))

(route 
  "btrie"
  (fn [o]
    {:type "text/plain"
     :content "hello"}))
