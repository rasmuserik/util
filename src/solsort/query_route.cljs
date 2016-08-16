(ns solsort.query-route
  (:require-macros
   [reagent.ratom :as ratom :refer [run!]])
  (:require
   [cljs.reader]
   [clojure.walk]
   [solsort.misc :refer [throttle hex-byte]]
   [solsort.appdb :refer [db db! db-async!]]))

(defonce use-query (atom false))
(defonce path (atom [:route]))
(defonce prev-url (atom nil))

(defn add-history "Add the current route the the history"
  ([]
   (when-not (= @prev-url js/location.href)
     (reset! prev-url js/location.href)
     (js/history.pushState nil nil js/location.href)))
  ([o]
   (db! @path o)
   (add-history)))
(def throttled-add-history "add-history, but executing at most once every other second"
  (throttle add-history 2000))
(defn set-route [o] (db! @path o))
(defn route [] (db @path))
(defn route-url [o]
  (str
   (re-find #"[^#?]*" js/location.href)  (if @use-query "?" "#")
   (clojure.string/replace
    (JSON.stringify (clj->js o))
    (js/RegExp. "[%#&?]" "g")
    (fn [c] (str "%" (hex-byte (.charCodeAt c 0)))))))
(defn set-route-path [p] (reset! path p))

(run! ; update url when :route changes
 (let [new-url (route-url (db @path))]
   (when (and (db @path)
              (not (= js/location.href new-url)))
     (js/history.replaceState nil nil new-url)
     (throttled-add-history))))

(defn- url-change "update (db @path) when url changes" []
  (db! @path
       (try
         (clojure.walk/keywordize-keys
          (js->clj
           (JSON.parse
            (js/decodeURIComponent
             (or (second (re-find #"[?#](.*)" js/location.href)) "")))))
         (catch js/Object e nil))))
(url-change)
(aset js/window "onpopstate" url-change)
