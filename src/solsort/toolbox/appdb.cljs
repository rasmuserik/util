(ns solsort.toolbox.appdb
  (:require-macros
   [reagent.ratom :as ratom :refer  [reaction]])
  (:require
   [solsort.toolbox.misc :refer [next-tick]]
   [reagent.core :as reagent :refer []]))

(defonce db-atom (reagent/atom {}))
(declare db-impl)
(defn- db-raw [& path]
  (if path
    (reaction (get @(apply db-impl (butlast path)) (last path)))
    db-atom))
(def db-impl
  "memoised function, that returns a subscription to a given path into the application db"
  (memoize db-raw))

  (defn db! "Write a value into the application db"
    ([path] (swap! db-atom update-in (butlast path) dissoc (last path)))
    ([path value] (swap! db-atom assoc-in path value) value))
(defn db-async! [path value] (next-tick #(db! path value)))
(defn db
  ([] @db-atom)
  ([path] @(apply db-impl path))
  ([path default]
   (let [val @(apply db-impl path)]
     (if (nil? val) default val))))

