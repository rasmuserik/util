(ns ^:figwheel-always solsort.util
  (:require
    [solsort.misc :as misc]
    [solsort.style :as style]
    [reagent.ratom :as ratom :refer-macros [reaction]]
    ))

(enable-console-print!)
(def db )

;; # style
(def normalize-css style/normalize-css)
(def grid style/grid)
(def css-name style/css-name)
(def handle-rule style/handle-rule)
(def handle-block style/handle-block)
(def clj->css style/clj->css)
(def js->css style/js->css)
(def load-style! style/load-style!)
(def style-tag style/style-tag)

;; # misc
;; ## js utils
(def next-tick misc/next-tick)
(def run-once misc/run-once)
(def parse-json-or-nil misc/parse-json-or-nil)
(def jsextend misc/jsextend)
(def html-data misc/html-data)
(def starts-with misc/starts-with)
(def function? misc/function?)
(def parse-path misc/parse-path)
(def canonize-string misc/canonize-string)
(def swap-trim misc/swap-trim)
(def hex-color misc/hex-color)
(def unique-id misc/unique-id)

;; ## misc js/clj
(def js-seq misc/js-seq)
(def <blob-url misc/<blob-url)
(def <blob-text misc/<blob-text)
(def unatom misc/unatom)

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
