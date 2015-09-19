;; # Router design
;;
;; There are two kinds of execution of routes:
;;
;; - stateful/reactive, ie. app(through hash-url, or call), widget, ...
;; - pure, ie. http-request, fn-call(rest/rpc/local), ...
;;
;; Several routes can be executed at the same time, ie. several widgets on a page, or
;; several parallel http-requests.
;;
;; A route is defined by: `(route "path" f)` where `f` is a function that takes an 
;; `options`-object as parameter, and returns a `result` object.
;; `options` include
;;
;; - `:reactive` - whether the result will be shown reactively, or single run
;; - `:id` - identifier for route execution, data specific to this execution should be 
;;   altered in `(db (:id options))`
;; - `:args` data specific to this execution
;; - `:path` path for this specific execution
;; - Probably later:
;;   `:accept` map of content-type/priorities, ie: `{"text/html" 0.9 "text/*" 0.1}`
;;
;; `result` is unwrapped async/atom and can include:
;;
;; - `:type` can be `:html`, or content-type-string if `:content` is raw data, 
;;   probably later also `:clj` `:json`, ... later on.
;; - dependse on type, ie. 
;;   - string-content-type: `:content` 
;;   - `:html` `:html` + `:title`  and optionally `:css`
;; - Probably later: `:caching`
;;
;; # Namespace definition and dependencies
(ns solsort.router
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [re-frame.core :as re-frame :refer [register-sub subscribe register-handler dispatch dispatch-sync]]
    [solsort.style :refer [default-style-str]]
    [solsort.misc :as misc :refer [function? chan? log]]
    [reagent.core :as reagent :refer  []]))

;; # Util

(defn js-seq [o] (seq (js/Array.prototype.slice.call o)))
(defn starts-with [string prefix] (= prefix (.slice string 0 (.-length prefix))) )
(defn html-data [elem]
  (into {} (->> (js-seq (.-attributes elem))   
                (map (fn [attr] [(.-name attr) (.-value attr)]))  
                (filter (fn [[k w]] (starts-with k "data-")))
                (map (fn [[k w]] [(.slice k 5) w])))))
(def route-re #"([^?]*)(.*)")
(defn parse-url [adr]
  (let [path (nth (re-matches route-re adr) 1)
        args (.split (.slice adr (inc (.-length path))) "&")
        args (map #(let [i (.indexOf % "=")]
                     (if (= -1 i)
                       [% true]
                       [(.slice % 0 i)
                        (.slice % (inc i)) ]))
                  args)]
    (into {"route" path} args)))

;; # Route management
(defonce routes (atom {}))
(defn route [id f] (swap! routes assoc id f))

;; # Dispatch-types
;;
;; There are the following possible dispatch-types:
;;
;; - fullpage app, initiated through url-hash, ie: `http://ur.l/#solsort:some/path&some=data`
;; - widget, with element like 
;;   `<div class="solsort-widget" data-route="some/path" data-some='data'>`
;; - static request, returns chan `(<dispatch-route {"route" "some/path", "some" "data"})`
;;   - this is used by solsort/net etc. for http-requests
;;
;; Later:
;; - actual url on certain domains, ie. solsort.com etc.
;;
;; NB: `(solsort.router/start)` should be called on page load, and on any page-change
;; that might add widgets.
;;
(defn <extract-route [data]
  (go (let [content (get @routes (get data "route" "")  
                         {:type "text/plain" :content "not found"})
            content (if (function? content) (content data) content)]
        (if (chan? content) (<! content) content))))
(defn start []
  (when (starts-with js/location.hash "#solsort:")
    (let [elem (or (js/document.getElementById "solsort-app-container")
                   (doto (js/document.createElement "div") 
                     (.setAttribute "id" "solsort-app-container")
                     (.setAttribute "class" "solsort-widget")
                     (js/document.body.appendChild)))  
          args (parse-url (.slice js/location.hash 9))]
      (doall (for [[k v] args] (.setAttribute elem (str "data-" k) v)))))
  (doall 
    (for [elem (js-seq (js/document.getElementsByClassName "solsort-widget"))]
      (go (let [data (<! (<extract-route (html-data elem)))]
            (if (= :html (:type data))
              (reagent/render-component (:html data) elem)
              (reagent/render-component [:pre (:content data)] elem)))))))
(defn html->content [data]
  (str
    "<!DOCTYPE html><html><head>"
    "<title>" (:title data) "</title>"
    "<meta charset='UTF-8'>"
    "<meta name='viewport' content='width=device-width, initial-scale=1'>"
    "<meta name='apple-mobile-web-app-capable' content='yes'>" 
    "<meta name='mobile-web-app-capable' content='yes'>" 
    "<style>" (default-style-str) "</style>"
    ;"<link href='//solsort.com/style.css' rel='stylesheet'>"
    "</head><body>"
    (reagent/render-to-string (:html data))
    "TODO-script-solsort.js"
    "</body></html>"))
(defn <http-route [data]
  (go (let [data (<! (<extract-route data))]
        (if (not= :html (:type data)) 
          data 
          {:type "text/html"
           :content (html->content data)}))))
;; # actual routes
(route "style" (fn [] {:type "text/css"   :content (default-style-str)}))
