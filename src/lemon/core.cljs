; # Lemon
;
; Lemon is the initial prototype of an app for tinkuy.
; More notes/documentation to come here real soon.
;
; ## Backlog
;
; -
;
; ## Build commands
;
; - `lein npm install` installs dependencies
; - `lein figwheel` starts development server on [http://localhost:3449](http://localhost:3449/) with nrepl on port 7888.
; - `lein clean` removes artifacts etc
; - `lein kibit` and `lein bikeshed -m 1000` runs various style tests
; - `lein cljsbuild once dist` builds minified version
; - `lein gendoc` regenerate project README.md from literate source code
; - TODO `lein cljsbuild test` builds and run unit-tests
;
; ## Random ideas
;
; - selemium tests
;
; # Dependency declarations
(ns ^:figwheel-always lemon.core
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [garden.core :refer [css]]
    [garden.units :refer [px]]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(enable-console-print!)

; # Style
; ## Viewport
;
(defonce viewport 
  (reagent/atom {}))

(defn update-viewport []
  (swap! viewport assoc :width js/window.innerWidth)
  (swap! viewport assoc :height js/window.innerHeight))

(js/window.addEventListener "resize" update-viewport)
(update-viewport)

; ## List of styles
;
(def styles (reagent/atom []))
(defn add-style [f] (swap! styles conj f))

; ## Reactive application of styles
(def style
  (ratom/reaction
    (print @styles)
    (reduce into [] (map #(% @viewport) @styles))))

(ratom/run!
  (print 'style @style (css @style))
  (aset
    (or (js/document.getElementById "solsort-style")
        (let [elem (js/document.createElement "style")]
          (aset elem "id"  "solsort-style")
          (js/document.head.appendChild elem)
          elem))
    "innerHTML"
    (css @style)))

; ## Actual styles

(add-style 
  (fn [viewport]
    [[:body {:background "cyan"}]
     [:h1 {:font-size (:height viewport)
          :position :absolute
          :left (px (/ (:width viewport) 2))
          }]]))

(add-style 
  (fn [viewport]
    [["@font-face"
     {:font-family "Ubuntu"
      :font-weight 400
      :src "url(font/latin/ubuntu-regular.ttf)format(truetype)"}]
     [:body {:font-family ["ubuntu", "sans-serif"]}]
     ]))
; # App-state
(defonce app-state
  (reagent/atom
    {:path ["index"]
     }))

; # Actual html
(defn front-page []
  [:div
   [:h1 "hello"]])

(defn main []
  (case (first (:path @app-state))
    "index" (front-page))
  )

; # Container etc.

(defn stylefn []
  (print 'stylefn @style)
  [:style 
   {:dangerouslySetInnerHTML
    #js{:__html (css @style)}
    }])
(defn root-elem []
  [:div
   [:h1 "hi"]
   [main]
   ]
  )
(reagent/render-component root-elem js/document.body)

(defn on-js-reload [])

; # test-test
(deftest dummy-test
  (testing "dummy description"
    (is  (= 1 2))))
