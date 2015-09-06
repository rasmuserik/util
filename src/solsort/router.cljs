(ns solsort.router
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [re-frame.core :as re-frame :refer [register-sub subscribe register-handler dispatch dispatch-sync]]
    [solsort.util :as util :refer [function? chan? log]]
    [solsort.ui :as ui]
    [solsort.style :as style]
    [reagent.core :as reagent :refer  []]))

(register-handler :error (fn [db [_ e] _] (log 'error (.-message e) e) db))
(register-handler :route (fn [db [_ route] _] (into db route)))
(defonce initialise
  (do
    (js/window.addEventListener "error" #(dispatch [:error %]))))
(register-sub :db (fn [db _] (reaction @db)))
#_(js/console.log (clj->js @(subscribe [:db]))) ; debug
(register-sub :app (fn [db _] (reaction (first (:path @db)))))

;; # router
(defonce routes (atom {}))
(defn route [id f]
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
  (or  (@routes @(subscribe [:app])) (:default @routes) #{}))

(keys @routes)

(defn main-app [content type]
  (let [content (if (satisfies? IAtom content) @content content)]
    (case type
      (:app) [ui/app content]
      (do
        (log 'unsupported-app-type (:type content))
        [:h1 "unsupported app type"]))))

(defn dispatch-route []
  (let [adr (or
              (and (= "#solsort:"  (.slice js/location.hash 0 9))
                   (.slice js/location.hash 9))
              (and (or (= js/location.port 1234)
                       (= js/location.hostname "solsort.com")
                       (= js/location.hostname "blog.solsort.com")
                       (= js/location.hostname "localhost"))
                   (js/location.pathname)))
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
        (when elem
          (reagent/render-component  [main-app content type] elem))))))

;; # routes

(route 
  "style"
  (fn []
    {:type :http
      :http-headers {:Content-Type "text/css"} 
      :content (style/clj->css @style/default-style)}))
