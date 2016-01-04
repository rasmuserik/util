(ns solsort.apps.experiments
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [route log unique-id]]
    [solsort.ui :refer [app input]]
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
(route "hello" (app
                 {:title (reaction (str "solsort " @(subscribe [:form-value "title"])))
                  :navigate-back {:event ['home] :title "Home" :icon "emojione-lemon"}
                  :actions [{:event [:log "pressed hello"] :icon "emojione-airplane"}
                            {:event ['paste] :icon "paste"} ]
                  :views [{:event ['view-left] :icon "left"}
                          {:event ['view-right] :icon "right"} ]
                  :html
                  [:div
                   "title: " [input :type "text" :name "title"]
                   [log-elem]]}))
