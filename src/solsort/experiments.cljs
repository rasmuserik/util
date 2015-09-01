(ns solsort.experiments
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.core :refer [route log]]
    [garden.core :refer [css]]
    [garden.units :refer [px em]]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

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
