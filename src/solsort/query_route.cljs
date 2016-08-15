(ns solsort.query-route
  (:require-macros
   [reagent.ratom :as ratom :refer [run!]])
  (:require
   [cljs.reader]
   [solsort.misc :refer [throttle]]
   [solsort.appdb :refer [db db! db-async!]]))

(defonce prev-url (atom nil))
(defn add-history []
  (when-not (= @prev-url js/location.href)
    (reset! prev-url js/location.href)
    (js/history.pushState nil nil js/location.href)))
(def throttled-history (throttle add-history 2000))
(defn- url-change []
  (js/console.log (js/decodeURIComponent
                   (or (second (re-find #"\?q=([^#]*)" js/location.href)) "{}")))
  (db! [:query-route]
       (try
         (cljs.reader/read-string
           (js/decodeURIComponent
            (or (second (re-find #"\?q=([^#]*)" js/location.href)) "{}}")))
         (catch js/Object e {}))))
(defn- db-change []
  (let [new-url (str
                (re-find #"[^#?]*" js/location.href)  "?q="
                (js/encodeURIComponent
                 (prn-str (db [:query-route]) )))
        ]
    (js/console.log "new-url" new-url (clj->js (db [:query-route] {})))
    (when-not (= js/location.href new-url)
      (js/history.replaceState nil nil new-url)
      (throttled-history))))

(url-change)
(aset js/window "onpopstate" url-change)

(defonce query-route-runner
  (run! (db-change)))
