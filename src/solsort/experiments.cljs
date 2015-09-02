(ns solsort.experiments
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.core :refer [route log]]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(deftest a-test
  (is (= 1 2)))

(run-tests)
(def ra (reagent/atom {}))

(def re (ratom/run!
          (do
            (print 'here @ra)
            (:hi @ra)
            )))


(print 'a @re)
(swap! ra assoc :hi "foo")
(print 'b)
(print 'c @re)
