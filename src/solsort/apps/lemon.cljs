(ns solsort.apps.lemon
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [route log <ajax host]]
    ;[garden.core :refer [css]]
    ;[garden.units :refer [px em]]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

#_
(
(enable-console-print!)
(js/console.log "in-lemon")

;; # Style
;; ## Viewport
;;
(defonce viewport
  (reagent/atom {}))

(defn update-viewport []
  (swap! viewport assoc :width js/window.innerWidth)
  (swap! viewport assoc :height js/window.innerHeight))

(js/window.addEventListener "resize" update-viewport)
(update-viewport)

;; ## List of styles
;;
(def styles (reagent/atom []))
(defn add-style [f] (swap! styles conj f) f)

;; ## Reactive application of styles
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
    "innerHTML" (css @style)

    ))

;; ## Actual styles
(add-style
  (ratom/reaction
    [["@font-face"
      {:font-family "Ubuntu"
       :font-weight 400
       :src "url(fonts/latin/ubuntu-regular.ttf)format(truetype)"}]
     [:body {:font-family ["ubuntu", "sans-serif"]
             }]
     ]))

;; # Reactions / data views
(def events
  (ratom/reaction (:events @state)))

;; # Actual html
(defn show-event [event]
  [:span (str
           (keys event)
           (event "startdate")
           )]
  )
(def title (ratom/reaction (:title @state)))
(defn front-page []
  [:div
   [:h1 @title]
   [:form {:action (str host "db/_session") :method "POST"}
    [:input {:name "name" :value "daemon"}]
    [:input {:name "password" :value (js/location.hash.slice 1)}]
    [:input {:type "submit"}]
    ]
   (into [:div ]
         (map show-event @events)
         )
   [:div "event-count" (str (count (:events @state)))]
   [:div (str (range 1000))]
   ])

(defn main []
  (log "hore" (:path @state))
  (case (first (:path @state))
    "lemon" (front-page))
  )

;; # Container etc.
;; ## Cursors
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
(def link-color "#88f")
;; ## style
;; ### hamburger-style
(def burger-style
  (add-style
    (ratom/reaction
      (let [burger-size 1
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
           :background link-color
           :border-radius (em burger-unit)
           :left 0
           :transition "0.4s ease-in-out"
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
         [".burger.cross>div:nth-child(3)" {:transform "rotate(-135deg)"}]]))))
;; ### top-bar style
(def bar-height 44)
(add-style
  (ratom/reaction
    (let [unit #(px (* bar-height %))
          bar-height 1
          bar-text .5
          margin (/ (- bar-height bar-text) 2)
          padding .15]
      [[:.top-bar
        {:text-align :center
         :margin-top (unit margin)
         :font-size (unit bar-text)}]
       [:.top-bar>.title
        {:display :inline-block
         :height (unit bar-text)
         :margin (unit 0)
         :padding [[(unit margin)
                    (unit (/ (- margin padding) 2))
                    (unit (/ (- margin padding) 2))
                    0]]
         :line-height (unit bar-text) } ]
       [:.top-bar>.topbutton>img
        {:height (unit bar-text)}]
       [:.float-left {:float :left }]
       [:.float-right {:float :right}]
       [:.topbutton
        {:background "#fff"
         :color link-color
         :border-radius (unit (/ 1 6))
         :display :inline-block
         :cursor :pointer
         :height (unit bar-text)
         :width (unit bar-text)
         :padding (unit padding)
         :margin [[(unit (- padding))
                   (unit (/ (- margin padding) 2))
                   (unit (/ (- margin padding) 2))
                   (unit (- margin padding))]]
         :box-shadow [["0px 0px 1.5px " "#00f"]]}]
       [:.bar-clear {:height (unit bar-height)}]])))
;; ### menu style
(add-style
  (ratom/reaction
    [[:.menu
      {:position :fixed
       :top 0
       :left 0
       :width "100%"
       :height "100%"
       :background "rgba(255,255,255,.92)"
       :box-shadow "0px 2px 4px rgba(0,0,0,.3)"
       :transition "0.4s ease-in-out"
       :overflow :hidden
       }]
     [:.hidden-menu
      {:height (px bar-height)}]]))
;; ## html
(defonce show-menu (reagent/atom true))
(defn root-elem []
  [:div
   [(if @show-menu :div.menu :div.menu.hidden-menu)
    [:div.top-bar
     [:a.float-right.topbutton {:on-click #(reset! show-menu (not @show-menu))}
      [(if @show-menu :div.burger.cross :div.burger) {:id "burger"} [:div] [:div] [:div]]]
     [:a.float-left.topbutton [:b "‹‹"]]
     ;[:a.float-left.topbutton [:img {:src "solsort.svg"}]]
     "Lemon" " hello " @width]
    [:ul
     [:li "hello"]
     [:li "world"]
     [:li "bye"]
     [:li "bye"]]]
   [:div.content
    [:div.bar-clear]
    [main]]])

(route "lemon" :app 
       (fn []
                 (log 'lemon-route)
                 (reagent/render-component [root-elem] js/document.body))) ; ##
;; # Get data from server
(defn load-events [server]
  (go
    (let [events (<! (<ajax (str "http://" server "/events.json") :result "text"))]
      (when events
        (swap! state assoc :events (js->clj (js/JSON.parse events)))))))
;(load-events "localhost:3000")
;(load-events "tinkuy.dk")

;; # test-test
(deftest dummy-test
  (testing "dummy description"
    (is  (= 1 2))))

;; # Daemon server
(js/socket.removeAllListeners "http-request")
(js/socket.removeAllListeners "http-response-log")
(js/socket.removeAllListeners "socket-connect")
(js/socket.removeAllListeners "socket-disconnect")
(js/socket.on
  "http-request"
  (fn [o] (js/console.log "http-request" o)
    (js/socket.emit
      "http-response"
      #js {:url (aget o "url")
           :key (aget o "key")
           :content (str "Hello " (aget o "url"))})
    ))
(js/socket.on
  "http-response-log"
  (fn [o] (js/console.log "http-response" o)))
(js/socket.on
  "socket-connect"
  (fn [o] (js/console.log "connect" o)))
(js/socket.on
  "socket-disconnect"
  (fn [o] (js/console.log "discon" o)))

(js/p2p.on
  "ready"
  (aset js/p2p "usePeerConnection" true)
  ;(js/p2p.emit "hello" #js {:peerId js/navigator.userAgent})
  )

(js/p2p.removeAllListeners "hello")
(js/p2p.on
  "hello"
  (fn [o] (print o))
  )
#_(go (loop [i 0]
        (js/p2p.emit "hello" #js {:peor (str js/navigator.userAgent)})
        (<! (timeout 1000))
        (recur (inc i))))
) ; #
