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
    [solsort.misc :as misc :refer [function? chan? log]]
    [solsort.ui :as ui]
    [solsort.style :as style]
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
(declare get-route)
(defn <extract-route [data]
  (go (let [content (get-route (get data "route" ""))
            content (if (function? content) (content data) content)
            ]
        (if (chan? content) (<! content) content))))
(defn start []
  (when (and (starts-with js/location.hash "#solsort:")
             (nil? (js/document.getElementById "solsort-app-container")))
    (let [elem (js/document.createElement "div")
          args (parse-url (.slice js/location.hash 9))]
      (.setAttribute elem "id" "solsort-app-container")
      (.setAttribute elem "class" "solsort-widget")
      (doall (for [[k v] args] (.setAttribute elem (str "data-" k) v)))
      (.appendChild js/document.body elem)))
  (doall 
    (for [elem (js-seq (js/document.getElementsByClassName "solsort-widget"))]
      (go (let [data (<! (<extract-route (html-data elem)))]
            (if (= :html (:type data))
              (reagent/render-component (:html data) elem)
              (reagent/render-component [:pre (:content data)] elem) 
              ))))))
(defn html->content [data]
  (str
    "<!DOCTYPE html><html><head>"
    "<title>" (:title data) "</title>"
    "<meta charset='UTF-8'>"
    "<meta name='viewport' content='width=device-width, initial-scale=1'>"
    "<meta name='apple-mobile-web-app-capable' content='yes'>" 
    "<meta name='mobile-web-app-capable' content='yes'>" 
    "<link href='normalize.css' rel='stylesheet' type='text/css'>"
    "</head><body>"
    "TODO-style"
    (reagent/render-to-string (:html data))
    "TODO-script-solsort.js"
    "</body></html>"))
(defn <dispatch-route [data]
  (go (let [data (<! (<extract-route data))]
        (if (not= :html (:type data)) (:content data) (html->content data)))))
(go (log (<! (<dispatch-route {"route" "hello"}))))
;; # Route management
(defn get-route [] 
  (fn [_]
    (go
      {:type :html
       :content "hello world"
       :title "hi"
       :html [:h1 "hello"]})))

;; # Handlers and subscriptions

(register-handler :route (fn [db [_ route] _] (into db route)))
(register-sub :app (fn [db _] (reaction (first (:path @db)))))

;; # router
(defonce routes (atom {}))
(defn route [id & {:keys (html app json http f)
                   :or {f (or html app json http)}}]
  (swap! routes assoc id f))

(defn parse-route [adr]
  (let [path (nth (re-matches route-re adr) 1)
        args (.split (.slice adr (inc (.-length path))) "&")
        path (.split path #"[./]")
        args (map #(let [i (.indexOf % "=")]
                     (if (= -1 i)
                       [% true]
                       [(.slice % 0 i)
                        (.slice % (inc i)) ]))
                  args)
        args (into {} args) ]
    {:path path :args args}) )

#_(defn get-route []
    (or  (@routes @(subscribe [:app])) (:default @routes) #{:disabled true}))

(keys @routes)

(defn main-app [content type]
  (let [content (if (satisfies? IAtom content) @content content)]
    (case type
      (:app) [ui/app content]
      (do
        (log 'unsupported-app-type (:type content))
        [:h1 "unsupported app type"]))))

#_(defn start []
    (let [adr (or
                (and (= "#solsort:"  (.slice js/location.hash 0 9))
                     (.slice js/location.hash 9))
                (and (or (= js/location.port 1234)
                         (= js/location.hostname "solsort.com")
                         (= js/location.hostname "blog.solsort.com")
                         (= js/location.hostname "localhost"))
                     js/location.pathname))
          route (and adr (parse-route adr))
          ]
      (dispatch-sync [:update-viewport])
      (dispatch-sync [:route route])
      (go
        (let  [elem  (js/document.getElementById "solsort-app")
               content (get-route)
               content (if (function? content) (content) content)
               content (if (chan? content) (<! content) content)
               type (:type (if (satisfies? IAtom content) @content content))
               ]
          (when (and elem (not (:disabled content)))
            (reagent/render-component  [main-app content type] elem))))))

;; # Actual routes for depended on code
;;
;; Modules used by the router cannot declare routes themself, so instead those routes 
;; are implemented here.

(route 
  "style" :http
  (fn []
    {:type :http
     :http-headers {:Content-Type "text/css"} 
     :content (style/clj->css @style/default-style)}))
