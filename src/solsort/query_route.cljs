(ns solsort.query-route
  (:require-macros
   [reagent.ratom :as ratom :refer [run!]])
  (:require
   [cljs.reader]
   [clojure.walk]
   [solsort.misc :refer [throttle]]
   [solsort.appdb :refer [db db! db-async!]]))

(defonce use-query (atom false))
(defonce prev-url
  (atom nil))
(defn add-history "Add the current route the the history"
  []
  (when-not (= @prev-url js/location.href)
    (reset! prev-url js/location.href)
    (js/history.pushState nil nil js/location.href)))
(def throttled-add-history "add-history, but executing at most once every other second"
  (throttle add-history 2000))

(defn hex-byte [i] (.slice (.toString (bit-or i 256) 16) -2))
(run! ; update url when :route changes
 (let [new-url (str
                (re-find #"[^#?]*" js/location.href)  (if @use-query "?" "#")
                (clojure.string/replace
                 (JSON.stringify (clj->js (db [:route])))
                 (js/RegExp. "[%#&?]" "g")
                 (fn [c] (str "%" (hex-byte (.charCodeAt c 0))))))]
   (when (and (db [:route])
              (not (= js/location.href new-url)))
     (js/history.replaceState nil nil new-url)
     (throttled-add-history))))

(defn- url-change "update (db :route) when url changes" []
  (db! [:route]
       (try
         (clojure.walk/keywordize-keys
          (js->clj
           (JSON.parse
            (js/decodeURIComponent
             (or (second (re-find #"[?#](.*)" js/location.href)) "")))))
         (catch js/Object e nil))))
(url-change)
(aset js/window "onpopstate" url-change)
