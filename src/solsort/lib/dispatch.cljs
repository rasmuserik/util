(ns solsort.dispatch
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require
    [solsort.sys.mbox :refer [local-mbox? call local local-mboxes log]]
    [solsort.sys.util :refer [chan? parse-path]]
    [solsort.lib.html :refer [render-html]]
    [solsort.sys.platform :refer [is-browser set-immediate]]
    ))

(enable-console-print!)


(defn dispatch []
  (go
    (let [args
          (or
            (and (exists? js/global) js/global.process (.slice js/global.process.argv 2))
            (and (exists? js/window) js/window js/window.location (parse-path js/window.location.hash)))]
      (log 'routes 'starting args)
      (if (local-mbox? (get args 0))
        (let [result (apply call local args)
              result (if (chan? result) (<! result) result)]
          (if (and is-browser (= "html" (:type result)))
            (render-html result))
          result)
        (log 'routes 'no-such-route args (local-mboxes))))))

(set-immediate dispatch)
(if is-browser
  (aset js/window "onhashchange" dispatch))
