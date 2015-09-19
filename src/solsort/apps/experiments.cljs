(ns solsort.apps.experiments
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [route log unique-id]]
    [solsort.ui :refer [app]]
    [reagent.core :as reagent :refer []]
    [re-frame.core :as re-frame :refer [subscribe]]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(deftest a-test
  (is (= 1 1)))

;(run-tests)

;(print 'a @re)
;(swap! ra assoc :hi "foo")
;(print 'b)
;(print 'c @re)

(defn log-elem []
  [:div (map (fn [e] [:div {:key (unique-id)} (.slice (str e) 1 -1)]) 
             (reverse @(subscribe [:log])))])
;; # Sample app
(route "hello" (app
                 {:title "solsort"
                  :navigate-back {:event ['home] :title "Home" :icon "home"}
                  :actions [{:event [:log "pressed hello"] :icon "hello"}
                            {:event ['paste] :icon "paste"} ]
                  :views [{:event ['view-left] :icon "left"}
                          {:event ['view-right] :icon "right"} ]
                  :html
                  [:div
                   [log-elem]]}))
