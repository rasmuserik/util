(ns solsort.query-route
  (:require-macros
   [reagent.ratom :as ratom :refer [run!]])
  (:require
   [cljs.reader]
   [solsort.misc :refer [throttle]]
   [solsort.appdb :refer [db db! db-async!]]))

(defonce prev-url
  (atom nil))
(defn add-history "Add the current route the the history"
  []
  (when-not (= @prev-url js/location.href)
    (reset! prev-url js/location.href)
    (js/history.pushState nil nil js/location.href)))
(def throttled-add-history "add-history, but executing at most once every other second"
  (throttle add-history 2000))

(run! ; update url when :route changes
 (let [new-url (str
                (re-find #"[^#?]*" js/location.href)  "?q="
                (js/encodeURIComponent
                 (prn-str (db [:route]))))]
   (when-not (= js/location.href new-url)
     (js/history.replaceState nil nil new-url)
     (throttled-add-history))))

(defn- url-change "update (db :route) when url changes" []
  (js/console.log (js/decodeURIComponent
                   (or (second (re-find #"\?q=([^#]*)" js/location.href)) "{}")))
  (db! [:route]
       (try
         (cljs.reader/read-string
          (js/decodeURIComponent
           (or (second (re-find #"\?q=([^#]*)" js/location.href)) "nil")))
         (catch js/Object e nil))))
(url-change)
(aset js/window "onpopstate" url-change)
