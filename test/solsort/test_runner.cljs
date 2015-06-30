(ns solsort.test-runner
  (:require
    [solsort.sys.util]
    [cljs.test :refer-macros [run-tests]]))

(enable-console-print!)
(js/setTimeout #(run-tests 'solsort.sys.util) 0)

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (println m)
  (let [success (if (cljs.test/successful? m) 0 1)]
  (when (exists? js/location)
    (js/document.write (str  "<iframe src=\"http://localhost:7357/" success m "\">")))
  (when (exists? js/process)
    ((aget js/process "exit") success))))
