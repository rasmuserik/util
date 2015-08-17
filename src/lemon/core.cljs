;; # Lemon
;;
;; Lemon is the initial prototype of an app for tinkuy.
;; More notes/documentation to come here real soon.
;; 
;; ## Build commands
;; 
;; - `lein npm install` install npm dependencies, specificly downloads normalize.css
;; - `lein figwheel` starts development server on [http://localhost:3449](http://localhost:3449/) with nrepl on port 7888.
;; - `lein clean` removes artifacts etc
;; - `lein marg` creates html documentation in `docs/`
;; - `lein kibit` and `lein bikeshed -m 1000` runs various style tests
;; - `lein cljsbuild once dist` builds minified version
;; - `lein cljsbuild test` builds and run unit-tests
;; - `lein gendoc` regenerate project README.md fromliterate source code
;; 
;; ## Random ideas
;; 
;; - selemium tests
;; 
;; # Dependency declarations
(ns ^:figwheel-always lemon.core
  (:require-macros
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(enable-console-print!)

;; # App-state
(defonce app-state
  (reagent/atom
    {:path ["index"]
     }))

;; # Main
(defn front-page []
  (print 'hi)
  [:div
   [:h1 "hello"]])

(defn main []
  (case (first (:path @app-state))
    "index" (front-page))
  )

(reagent/render-component main (.getElementById js/document "app"))

(defn on-js-reload [])

;; # test-test
(deftest dummy-test
    (testing "dummy description"
          (is  (= 1 2))))
