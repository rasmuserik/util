;; # Router - dispatch into different states of the application
;; 
;; Different kinds of dispatch:
;;
;; - http-requests - html, pages, rest
;; - widgets in page
;; - fullpage/url-hash/app ready/opened
;; - rpc/ipc
;;
;; NB: notice there can be severa widgets on a page or several http-requests, 
;; meaning that the db/state should anticipate that.
;;
;; A widget/page is a combination of route+db
;;
;; Different kinds of content are:
;;
;; - data with mime-type/caching/http-headers
;;   - json-data / api
;;   - (images rendered with canvas etc.)
;;   - css
;;   - pure html
;; - react-element
;;   - app
;;   - widget
;;
;; ----
;;
;; - Initialiser
;;   - HTTP-request
;;   - widget + call init()
;;   - (fullpage-load)
;;   - (rpc/ipc)
;; - Output
;;   - in-page reactive react component
;;   - static data/css/html
;;
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

;; # Actual Router implementation
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
