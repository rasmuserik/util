(ns solsort.lib.autorestart
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]])
  (:require
    [solsort.lib.net :refer [broadcast]]
    [solsort.sys.platform :refer [is-nodejs is-browser fs exit]]
    [solsort.sys.mbox :refer [handle log]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(when is-nodejs
  (.watch fs js/__filename
          (memoize (fn []
                     (broadcast "reload" nil)
                     (log 'system 'source-change 'restarting)
                     (exit 0)))))

(when is-browser
    (when (exists? js/applicationCache)
          (aset js/applicationCache "onupdateready" #(js/location.reload)))
      (handle "reload" #(go (<! (timeout 800)) (js/location.reload))))
