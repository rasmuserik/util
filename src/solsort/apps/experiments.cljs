(ns solsort.apps.experiments
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [route log unique-id]]
    [reagent.core :as reagent :refer []]
    [re-frame.core :as re-frame :refer [subscribe]]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(deftest a-test
  (is (= 1 1)))

(run-tests)
(def ra (reagent/atom {}))

(def re (ratom/run!
          (do
            ;(print 'here @ra)
            ;(:hi @ra)
            )))


;(print 'a @re)
;(swap! ra assoc :hi "foo")
;(print 'b)
;(print 'c @re)

;; # Sample app
(route "hello" :app
  (fn []
    (reaction {:type :app
           :title "solsort"
           :navigate-back {:event ['home] :title "Home" :icon "home"}
           :actions [ {:event [:log "pressed hello"] :icon "hello"}
                     {:event ['paste] :icon "paste"} ]
           :views [ {:event ['view-left] :icon "left"}
                   {:event ['view-right] :icon "right"} ]
           :html
           [:div
            (map (fn [e] [:div {:key (unique-id)} (.slice (str e) 1 -1)]) (reverse @(subscribe [:log])))
            (str (range 1000))]})))
