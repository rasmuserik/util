(ns ^:figwheel-always  solsort.util
  (:require-macros
   [cljs.core.async.macros :refer  [go go-loop alt!]])
  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :as async :refer  [>! <! chan put! take! timeout close! pipe]]
    [solsort.toolbox.ui :as ui]
    [solsort.toolbox.xml :as xml]
    [solsort.toolbox.misc :as misc]
    [solsort.toolbox.style :as style]
    [solsort.toolbox.net :as net]))

(enable-console-print!)

(defn log [& args]
  (js/console.log.apply js/console  (clj->js args))
  (first args))

;; # style
(def normalize-css style/normalize-css)
(def grid style/grid)
(def css-name style/css-name)
(def handle-rule style/handle-rule)
(def handle-block style/handle-block)
(def clj->css style/clj->css)
(def js->css style/js->css)
(def load-style! "(style, id) -> nil" style/load-style!)
(def style-tag style/style-tag)


;; # xml
(def dom->clj xml/dom->clj)
(def xml->sxml xml/xml->sxml)

;; # ui
(def html-data ui/html-data)
(def page-ready ui/page-ready)
(def render ui/render)

;; # misc
;; ## js utils
(def next-tick misc/next-tick)
(def run-once misc/run-once)
(def parse-json-or-nil misc/parse-json-or-nil)
(def jsextend misc/jsextend)
(def starts-with misc/starts-with)
(def function? misc/function?)
(def parse-path misc/parse-path)
(def canonize-string misc/canonize-string)
(def swap-trim misc/swap-trim)
(def hex-color misc/hex-color)
(def unique-id misc/unique-id)
(defn cryptohash [o] (str (hash o))) ; TODO expected collisionfree hash to string
(defn <load-script [url]
  (let [id (cryptohash [:load-script url])
        tag (or
             (js/document.getElementById id)
             (js/document.createElement "script"))
        c (chan)]
    (aset tag "id" id)
    (aset tag "src" url)
    (.addEventListener tag "error" #(put! c (js/Error %)))
    (.addEventListener tag "load " #(put! c true))
    (js/document.head.appendChild tag)))

;; ## misc js/clj
(def js-seq misc/js-seq)
(def <blob-url misc/<blob-url)
(def <blob-text misc/<blob-text)
(def unatom misc/unatom)
(def throttle misc/throttle)

;; ## Async
(def <p misc/<p)
(def <n misc/<n)
(def put!close! misc/put!close!)
(def chan? misc/chan?)
(def <seq<! misc/<seq<!)

;; ## transducers
(def transducer-status misc/transducer-status)
(def transducer-accumulate misc/transducer-accumulate)
(def group-lines-by-first misc/group-lines-by-first)
(def print-channel misc/print-channel)
(def by-first misc/by-first)

;; # net
(def <load-js net/<load-js)
(def utf16->utf8 net/utf16->utf8)
(def utf8->utf16 net/utf8->utf16)
(def buf->utf8-str net/buf->utf8-str)
(def buf->str net/buf->str)
(def utf8-str->buf net/utf8-str->buf)
(def str->buf net/str->buf)
(def <sha256 net/<sha256)
(def <sha256-str net/<sha256-str)
(def <ajax net/<ajax)

;; # from fm-tools - needs refactoring
(defn third [col] (nth col 2))
(defn delay-fn [f] (fn [& args] (next-tick #(apply f args))))
(defn <chan-seq [arr] (async/reduce conj nil (async/merge arr)))
(defn to-map
  [o]
  (cond
    (map? o) o
    (sequential? o) (zipmap (range) o)
    :else {}))
(defn timestamp->isostring [i] (.toISOString (js/Date. i)))
(defn str->timestamp [s] (.valueOf (js/Date. s)))

(defn tap-chan [m] (let [c (chan)] (async/tap m c) c))
(defn js-obj-push [obj k v] (.push (or (aget obj k) (aset obj k #js [])) v))
