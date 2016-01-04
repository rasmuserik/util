(ns solsort.sys.platform
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:require
    [solsort.sys.util :refer [unique-id]]
    [solsort.sys.mbox :refer [log]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(enable-console-print!)
(declare ensure-dir)



;; Global+predicates
(def global
  (cond
    (exists? js/window) js/window
    (exists? js/global) js/global
    (exists? js/self) js/self
    :else ((fn [] js/this))))
(def is-browser (and (exists? js/window) (exists? js/window.document)))
(def is-nodejs (and
                 (exists? js/global)
                 (.hasOwnProperty js/global "process")
                 (.hasOwnProperty js/global.process "title")))


;; File system
(def fs (if is-nodejs (js/require "fs")))
(defn ensure-dir [dirname] (if (not (.existsSync fs dirname)) (.mkdirSync fs dirname)))

(defn read-file-sync [filename] (.readFileSync (js/require "fs") filename))
(defn each-lines [filename]
  (let
    [c (chan 1)
     buf (atom "")
     stream (.createReadStream fs filename)]
    (.on stream "data"
         (fn [data]
           (.pause stream)
           (go
             (swap! buf #(str % data))
             (let [lines (.split @buf "\n")]
               (swap! buf #(aget lines (dec (.-length lines))))
               (loop [i 0]
                 (when (< i (dec (.-length lines)))
                   (>! c (str (aget lines i) "\n"))
                   (recur (inc i)))))
             (.resume stream)
             )
           ))
    (.on stream "close"
         (fn []
           (put! c @buf)
           (close! c)))
    c))


;; OS
(defn exec [cmd]
  (let [c (chan)]
    (.exec (js/require "child_process") cmd
           (fn [err stdout stderr]
             (if (nil? err)
               (put! c stdout)
               (close! c)
               )))
    c))

(defn exit [errcode]
  (go
    (<! (timeout 300))
    (log 'system 'exit errcode)
    (if is-nodejs
      (js/process.exit errcode))))

;; Browser API
(def origin (if is-nodejs "http://localhost:9999" js/location.origin))
(def XHR (if is-nodejs (aget (js/require "xmlhttprequest") "XMLHttpRequest") js/XMLHttpRequest))
(def set-immediate ; "execute function immediately after event-handling"
  (if (exists? js/setImmediate)
    js/setImmediate ; node.js and IE (IE might be buggy)
    (fn [f] (js/setTimeout f 0))))

(def worker
  (if is-nodejs
    (aget (js/require "webworker-threads") "Worker")
    (aget global "Worker")))


; react
(when (and is-nodejs (not is-browser))
  (aset global "localStorage"
        (let [module (js/require "node-localstorage")
              LocalStorage (aget module "LocalStorage")]
          (ensure-dir "./dbs/")
          (LocalStorage. "./dbs/localstorage")))
  (aset global "React" (js/require "react")))
