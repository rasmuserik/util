; # Lemon
;
; Lemon is the initial prototype of an app for tinkuy.
; More notes/documentation to come here real soon.
;
; ## Backlog
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
    [garden.units :refer [px em]]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(enable-console-print!)

; # Utility functions (to be merged into util)
(defn ajaxText "Do an ajax request and return the result as JSON" ; ## 
  [url]
  (let  [c  (chan)]
    (goog.net.XhrIo/send
      url
      (fn  [o]
        (when
          (and o  (.-target o))
          (put! c  (.getResponseText (.-target o))))
        (close! c)))
    c))
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
(defn add-style [f] (swap! styles conj f) f)

; ## Reactive application of styles
(def style
  (ratom/reaction
    (reduce into [] (map (fn [ratom] @ratom) @styles))))

(ratom/run!
  (aset
    (or (js/document.getElementById "solsort-style")
        (let [elem (js/document.createElement "style")]
          (aset elem "id"  "solsort-style")
          (js/document.head.appendChild elem)
          elem))
    "innerHTML"
    (css @style)))

; ## Actual styles

#_(add-style 
    (ratom/reaction
      [[:body {:background "cyan"}]]))

(add-style 
  (ratom/reaction
    [["@font-face"
      {:font-family "Ubuntu"
       :font-weight 400
       :src "url(fonts/latin/ubuntu-regular.ttf)format(truetype)"}]
     [:body {:font-family ["ubuntu", "sans-serif"]}]
     ]))

; # App-state
(defonce app-state
  (reagent/atom
    {:path ["index"]
     }))

; # Reactions / data views
(def events
  (ratom/reaction (:events @app-state)))
; # Actual html
(defn show-event [event]
  [:span (str 
           (keys event)
           (event "startdate")
           )]
  )
(defn front-page []
  [:div
   (into [:div ]
         (map show-event @events)
         )
   [:div "event-count" (str (count (:events @app-state)))]
   [:h1 "hello"]
   ])

(defn main []
  (case (first (:path @app-state))
    "index" (front-page))
  )

; # Container etc.
; ## Cursors
(def show-menu (ratom/reaction (:show-menu @viewport)))
(def width (ratom/reaction (or (:width @viewport) 320)))
(def viewport-scale (ratom/reaction
                      (cond
                        (< @width 640) :small
                        (< @width 960) :medium
                        :else :large
                        )
                      ))
(def unit 
  (ratom/reaction (/ @width
                     (case @viewport-scale
                       :small 8
                       :medium 16
                       :large 24))))
(def border (ratom/reaction (/ @unit 40)))
(def link-color "#44f")
(ratom/run! (print 'blah @unit))
; ## style
; ### hamburger-style
(def burger-style (add-style
                    (ratom/reaction
                      (let [burger-size 0.8
                            burger-unit (/ burger-size 6)
                            ]
                        [[:.burger 
                          {:display :inline-block
                           :position :relative
                           :width (em burger-size)
                           :height (em burger-size)
                           }]
                         [".burger>div"
                           {:display :block
                            :position :absolute
                            :height (em burger-unit)
                            :width (em (* 6 burger-unit))
                            :background "#88f"
                            :border-radius (em burger-unit)
                            :left 0
                            :transition ".3s ease-in-out"
                            }]
                         [".burger>div:nth-child(1)" {:top (em (* 0.5 burger-unit))}]
                         [".burger>div:nth-child(2)" {:top (em (* 2.5 burger-unit))}]
                         [".burger>div:nth-child(3)" {:top (em  (* 4.5 burger-unit))}]
                         [".burger.cross>div"
                          {:width (em (* 7 burger-unit))
                           :top (em (* 2.5 burger-unit))
                           :left (em (* -0.5 burger-unit))}]
                         [".burger.cross>div:nth-child(1)" {:transform "rotate(135deg)"}]
                         [".burger.cross>div:nth-child(2)" 
                          {:left (em (* 2.5 burger-unit))
                           :width (em 0)
                           :transform "rotate(90deg)"
                           }]
                         [".burger.cross>div:nth-child(3)" {:transform "rotate(-135deg)"}]

                         ]
                        ))))
; ### top-bar style
(add-style 
  (ratom/reaction
    (let [unit #(px (* @unit %))
          bar-height 1
          bar-text .6
          margin (/ (- bar-height bar-text) 2)]
      (print (unit 3))
      (print @viewport-scale)
      [[:.top-bar
        {:text-align :center
         :background "rgba(255,255,255,0.90)"
         :position :fixed
         :top "0px"
         :width "100%"
         :font-size (unit bar-text)
         :height (unit bar-height)
         :box-shadow "2px 2px 4px rgba(0,0,0,.5)"
         :font-height (unit bar-text)
         }
        ]
       [:.top-bar>.elem
        {
         :display :inline-block
         :height (unit bar-text)
         :margin (unit 0)
         :padding (unit margin)
         :line-height (unit bar-text)
         }
        ]
       [:.top-bar>.action
        {
         :width (unit bar-text)
         :color link-color
         }
        ]
       [:.top-bar>.back {:float :left }]
       [:.top-bar>.menu {:float :right}]
       [:.bar-clear {:height (unit bar-height)}]

       ])


    ))

; ## html
(defonce is-cross (reagent/atom false))
(defn root-elem []
  [:div.root
   [:div.top-bar
    [:a.back.elem.action "<"]
    [:span.title.elem @width (name @viewport-scale)]
    [:a.menu.elem.action {
                     :on-click #(reset! is-cross (not @is-cross))
                     } 
     [(if @is-cross :div.burger.cross :div.burger) {:id "burger"} [:div] [:div] [:div]]
     ]
    ]
   #_[:div.menu
      [:ul
       [:li "hello"]
       [:li "world"]
       [:li "bye"]
       [:li "bye"] ]]
   [:div.bar-clear]
   [:div.content
    [main]]]
  )
(reagent/render-component [root-elem] js/document.body)

(defn on-js-reload [])

; # Get data from server
(defn load-events [server]
  (go 
    (let [events (<! (ajaxText (str "http://" server "/events.json")))]
      (when events
        (swap! app-state assoc :events (js->clj (js/JSON.parse events)))))))
(load-events "localhost:3000")
;(load-events "tinkuy.dk")

; # test-test
(deftest dummy-test
  (testing "dummy description"
    (is  (= 1 2))))

