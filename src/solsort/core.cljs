(ns solsort.core
  (:require
    [solsort.util :as util]
    [solsort.net :as net]
    [solsort.ui :as ui]
    [solsort.db :as db]
    [solsort.style :as style]
    [solsort.router :as router]
    ))

(def log util/log)
(def canonize-string util/canonize-string)
(def hex-color util/hex-color)
(def unique-id util/unique-id)

(def app ui/app)

(def route router/route)
(def dispatch-route router/dispatch-route)

(def ajax net/ajax)
