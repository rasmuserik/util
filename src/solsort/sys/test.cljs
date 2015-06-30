(ns solsort.sys.test
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:require
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))


(def testcases (atom []))
(defn testcase [id f]
  (swap! testcases conj [id f]))
