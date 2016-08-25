(ns solsort.toolbox.transit
  (:require
   [cognitect.transit :as transit]))

(defn clj->json [s] (transit/write (transit/writer :json) s))
(defn json->clj [s] (transit/read (transit/reader :json) s))
