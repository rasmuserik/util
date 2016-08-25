(ns solsort.toolbox.setup
  (:require
   [devtools.core :as devtools]))

(when js/window.applicationCache
  (aset js/window.applicationCache "onupdateready" #(js/location.reload)))
(defonce dev-tools (devtools/install!))
(.addEventListener js/window "error"
                   (fn [err] (js/console.log "Error" err)))
