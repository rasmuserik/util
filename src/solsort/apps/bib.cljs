(ns solsort.apps.bib
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [route log ajax]]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

; NB: http://ogp.me/, http://schema.org, dublin-core, https://en.wikipedia.org/wiki/RDFa
;; # old

(defn jsonp "Do an ajax request and return the result as JSON" ; ##
  [url]
  (let  [c  (chan)
         jsonp (goog.net.Jsonp. url)]
    (.send jsonp nil #(put! c %) #(close! c))
    c))
(defn jsonp-hack
  [url]
  (let [c (chan)]
    (aset js/window "jsonpcallback" (fn [o] (put! c o)))
    (.appendChild js/document.head
                  (let [elem (js/document.createElement "script")]
                    (set! (.-src elem) (str url "?callback=jsonpcallback"))
                    elem))
    c
    )
  )

(go
  (print (<! (ajax (str js/solsort_server "/db/_session") :result "text")))
  (js/console.log
    (clj->js
      {:info (<! (jsonp "http://localhost/bib/info/50581438"))
       :related (<! (jsonp "http://localhost/bib/related/50581438"))
       :triples (<! (jsonp-hack (str "https://dev.vejlebib.dk/ting-visual-relation"
                                "/get-ting-object/870970-basis:50581438")))}))
  )
(js/console.log "hello")

;; bibdata-process
(defn get-triple [id]
  (go
    (clj->js
      { :stat(<! (jsonp (str "http://localhost/bib/info/" id)))
       :related (<! (jsonp (str "http://localhost/bib/related/" id)))
       :info (<! (jsonp (str "http://localhost/bibdata/info/" id))) })

    ))

#_(go (let [lids (js/JSON.parse (<! (ajax (str js/solsort_server "/db/bib/info/lids.json")
                                          :result "text")))]
        (loop [i (or (int (js/localStorage.getItem "i")) 0)
               ]
          (when (<= i (count lids))
            (let [lid (aget lids i)
                  data (<! (get-triple  (aget lids i)))
                  result (<! (ajax
                               (str js/solsort_server "/db/bib/" lid)
                               :method "PUT"
                               :data (js/JSON.stringify data)))]
              (aset js/document "title" (str i))
              (js/localStorage.setItem "i" i)
              (recur (inc i)))))))
