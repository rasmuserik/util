(ns solsort.util
  (:require
    [solsort.misc :as misc]
    [solsort.net :as net]
    [solsort.ui :as ui]
    [solsort.db :as db]
    [solsort.style :as style]
    [solsort.router :as router]
    ))

(def log misc/log)
(def canonize-string misc/canonize-string)
(def hex-color misc/hex-color)
(def unique-id misc/unique-id)

(def app ui/app)

(def route router/route)
(def dispatch-route router/dispatch-route)

(def ajax net/ajax)
