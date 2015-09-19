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
;; - Probably later:
;;   `:auth` authentication
;;
;; `result` is unwrapped async/atom and can include:
;;
;; - `:type` can be `:html`, or content-type-string if `:content` is raw data, 
;;   probably later also `:clj` `:json`, ... later on.
;; - one of
;;   - `:content` 
;;   - `:html` + `:title`  and optionally `:css`
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

;; # Handlers and subscriptions

(register-handler :route (fn [db [_ route] _] (into db route)))
(register-sub :app (fn [db _] (reaction (first (:path @db)))))

;; # router
(defonce routes (atom {}))
(defn route [id & {:keys (html app json http f)
                   :or {f (or html app json http)}}]
  (swap! routes assoc id f))
(def route-re #"([^?]*)(.*)")

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

(defn get-route []
  (or  (@routes @(subscribe [:app])) (:default @routes) #{:disabled true}))

(keys @routes)

(defn main-app [content type]
  (let [content (if (satisfies? IAtom content) @content content)]
    (case type
      (:app) [ui/app content]
      (do
        (log 'unsupported-app-type (:type content))
        [:h1 "unsupported app type"]))))

(defn start []
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
