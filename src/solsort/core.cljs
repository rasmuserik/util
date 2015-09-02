(ns solsort.core
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [clojure.string :as string :refer  [split]]
    [clojure.string :refer  [join]]
    [cognitect.transit :as transit]
    [re-frame.core :as re-frame :refer [register-sub subscribe]]
    [goog.net.Jsonp]
    [goog.net.XhrIo]
    [reagent.core :as reagent :refer  []]))

(enable-console-print!)

(def is-figwheel (some? js/window.figwheel))
(when is-figwheel (js/setTimeout #(run-tests) 0))

;; # forward declarations
(declare app)
;; # logger
(defn log [& args] (apply print 'log args))

;; # DBs
;;
;; We have 3 need kinds of databases
;;
;; - local sync-able databases - pouchdb (currently backed by couchdb)
;; - search - elasticsearch
;; - central key-value store - with abstracted-api (currently backed by couchdb)
;;
;; ## Authentication
;;
;; "Databases" are databases in couchdb/pouchdb and indexes in elasticsearch
;;
;; A "list of users" is either a list of users or "all".
;;
;; Every database has three lists of users:
;;
;; - Readers, whom are allowed to read/query the database
;; - Writers, whom are allowed to write to the database
;; - Owners, whom are allowed to administer the database, including updating the userlist
;;
;; The "daemon" user, is the only one capable of creating new databases, and is also implicit
;; in the list of owners of all databases
;;
;; # re-frame - this section is replaced by "App" above.
;; ## Event/dispatch-mechanism
;;
;; We might have different states due to parallel async static content generation. This means
;; that async event handlers need to have a `dispatch` function supplied, for emitting events
;; in current content. So app-handlers is `app-db, event, dispatch-function -> app-db` instead
;; of `app-db, event -> app-db`. `solsort.core/handle` just accepts one of these functions, and
;; wrap custom middleware. Similarly `solsort.core/dispatch` can be called instead of
;; `re-frame.core/dispatch` during reactions, and automatically dispatches to the app-db of the
;; current reaction.
;;

(defn -create-dispatch-fn [db]
  (let [pid (or (:pid db) "pid")]
    (fn [event-id & args]
      (re-frame/dispatch (into [event-id pid] args)))))

(defn register-handler
  ; TODO and middleware removing pid, and optionally swapping db
  ([event-id f]
   (re-frame/register-handler event-id
                              (fn [db [event pid & args]] (f db args (-create-dispatch-fn db)))))
  ([event-id middleware f]
   (re-frame/register-handler event-id middleware
                              (fn [db [event pid & args]] (f db args (-create-dispatch-fn db)))))
  )
(defn dispatch [[event-id & args]]
  (re-frame/dispatch (into  [event-id @(subscribe [:pid])] args)))
(defn dispatch-sync [[event-id & args]]
  (re-frame/dispatch-sync (into  [event-id @(subscribe [:pid])] args)))


;; ## update-viewport
(defonce resize-listener (js/window.addEventListener "resize" #(dispatch [:update-viewport])))

;; TODO remove this:
(defonce state
  (reagent/atom
    {:path ["lemon"]
     :args {}
     :viewport
     {:width js/window.innerWidth
      :height js/window.innerHeight }}))

;; # ajax

(defn ajax [url & {:keys [method data headers timeout credentials result]
                   :or {method "GET"
                        data nil
                        headers #js {}
                        timeout 0
                        credentials true
                        result "js->clj"
                        }}]
  (let [c (chan)
        data-is-json (not (contains?
                            [nil js/window.ArrayBuffer js/window.ArrayBufferView js/window.Blob]
                            (type data)))
        data (if data-is-json (js/JSON.stringify (clj->js data)) data)]
    (when data-is-json
      (aset headers "Content-Type" "application/json"))
    (goog.net.XhrIo/send
      url
      (fn [o]
        (try
          (let [res (.getResponseText (.-target o))
                res (case (name result)
                      ("text") res
                      ("json") (js/JSON.parse res)
                      ("js->clj") (js->clj (js/JSON.parse res)))]
            (put! c res))
          (catch :default e
            (js/console.log e)
            (close! c))))
      method data headers timeout credentials)
    c))

(defonce gargs (atom {}))
(go
  (<! (ajax "http://localhost:1234/db/_session"
            :method "POST"
            :data {:name (@gargs "user") :password (@gargs "password")})))

;; # meta-functions
(defn run-once [f]
  (let [do-run (atom true)]
    (fn [& args]
      (when @do-run
        (reset! do-run false)
        (apply f args)))))

;; # util
;; ## js-json
(defn parse-json-or-nil [str]
  (try
    (js/JSON.parse str)
    (catch :default _ nil)))

(defn jsextend [target source]
  (let [ks (js/Object.keys source)]
    (while (pos? (.-length ks))
      (let [k (.pop ks)] (aset target k (aget source k)))))
  target)

;; ## tests
(defn chan? [c] (instance? ManyToManyChannel c))
(defn function? [c] (instance? js/Function c))
;; ## async channels

(defn go<!-seq [cs]
  (go
    (loop [acc []
           cs cs]
      (if (first cs)
        (recur (conj acc (<! (first cs)))
               (rest cs))
        acc))))

(defn print-channel [c]
  (go (loop [msg (<! c)]
        (when msg (print msg) (recur (<! c))))))


;; ## transducers
(defn by-first [xf]
  (let [prev-key (atom nil)
        values (atom '())]
    (fn
      ([result]
       (when (pos? (count @values))
         (xf result [@prev-key @values])
         (reset! values '()))
       (xf result))
      ([result input]
       (if (= (first input) @prev-key)
         (swap! values conj (rest input))
         (do
           (if (pos? (count @values)) (xf result [@prev-key @values]))
           (reset! prev-key (first input))
           (reset! values (list (rest input)))))))))

(defn transducer-status [& s]
  (fn [xf]
    (let [prev-time (atom 0)
          cnt (atom 0)]
      (fn
        ([result]
         (apply log (concat s (list 'done)))
         (xf result))
        ([result input]
         (swap! cnt inc)
         (when (< 60000 (- (.now js/Date) @prev-time))
           (reset! prev-time (.now js/Date))
           (apply log (concat s (list @cnt))))
         (xf result input))))))

(defn transducer-accumulate [initial]
  (fn [xf]
    (let [acc (atom initial)]
      (fn
        ([result]
         (when @acc
           (xf result @acc)
           (reset! acc nil))
         (xf result))
        ([result input]
         (swap! acc conj input))))))

(def group-lines-by-first
  (comp
    by-first
    (map (fn [[k v]] [k (map (fn [[s]] s) v)]))))

; string
(defn parse-path [path] (.split (.slice path 1) #"[/.]"))

(defn canonize-string [s]
  (.replace (.trim (.toLowerCase s))
            (js/RegExp. "(%[0-9a-fA-F][0-9a-fA-F]|[^a-z0-9])+", "g") "-"))
(defn swap-trim  [[a b]] [(string/trim b) (string/trim a)])


;; ## integers / colors
(defn hex-color [n] (str "#" (.slice (.toString (bit-or 0x1000000 (bit-and 0xffffff n)) 16) 1)))

;; ## unique id
(def -unique-id-counter  (atom 0))
(defn unique-id  []  (str "id"  (swap! -unique-id-counter inc)))

;; ## transit
;(def -writer  (transit/writer :json))
;(def -reader  (transit/reader :json))

;; # router
(defonce routes (atom {}))
(defn route [id f]
  (swap! routes assoc id f))
(def route-re #"([^?]*)(.*)")

(defn parse-route [adr]
  (let [path (nth (re-matches route-re adr) 1)
        args (.split (.slice adr (inc (.-length path))) "&")
        path (.split path #"[./]")
        args (map #(let [i (.indexOf % "=")]
                     (if (= -1 i)
                       [% true]
                       [(.slice % 0 i)
                        (.slice % (inc i)) ]))
                  args)
        args (into {} args) ]
    (reset! gargs args)
    {:path path :args args}) )

(defn get-route []
  (or  (@routes @(subscribe [:app])) (:default @routes) #{}))

(defn main-app [content type]
    (let [content (if (satisfies? IAtom content) @content content)]
    (case type
      (:app) [app content]
      (do
        (log 'unsupported-app-type (:type content))
        [:h1 "unsupported app type"]))))

(defn dispatch-route []
  (let [adr (or
              (and (= "#solsort:"  (.slice js/location.hash 0 9))
                   (.slice js/location.hash 9))
              (and (or (= js/location.port 1234)
                       (= js/location.hostname "solsort.com")
                       (= js/location.hostname "blog.solsort.com")
                       (= js/location.hostname "localhost"))
                   (js/location.pathname)))
        route (and adr (parse-route adr))]
    (dispatch-sync [:update-viewport])
    (dispatch-sync [:route route])
    (go
    (let  [elem  (js/document.getElementById "solsort-app")
           content (get-route)
            content (if (function? content) (content) content)
          content (if (chan? content) (<! content) content)
        type (:type (if (satisfies? IAtom content) @content content))
           ]
      (when elem
        (reagent/render-component  [main-app content type] elem))))))

;; # css
(defn css-name [id]
  (clojure.string/replace (name id) #"[A-Z]" #(str "-" (.toLowerCase %1))))
#_(testcase 'css-name
            #(= (css-name :FooBar) "-foo-bar"))
(defn handle-rule [[k v]]
  (str (css-name k) ":" (if (number? v) (str v "px") (name v))))
(defn handle-block [[id rules]]
  (str (name id) "{" (join ";" (map handle-rule (seq rules))) "}"))
(defn clj->css [o]
  (join (map str (seq o))) (join (map handle-block (seq o))))
(defn js->css [o] (clj->css (js->clj o)))

(testing "css"
  (is (= (clj->css {:h1 {:fontWeight :normal :fontSize 14} :.div {:background :blue}})
         "h1{font-weight:normal;font-size:14px}.div{background:blue}")))
(is (= (clj->css [[:h1 {:fontWeight :normal :fontSize 14}] 
                  [:.div {:background :blue}]
                  ["h1" {:background :red}]
                  ])
       "h1{font-weight:normal;font-size:14px}.div{background:blue}h1{background:red}")) 

(def default-style
  (atom { "@font-face" {:fontFamily "Ubuntu"
                        :fontWeight "400"
                        :src "url(/font/ubuntu-latin1.ttf)format(truetype)"}
         :.container {:margin "5%" }
         :.button {:margin 5 :padding 5 :borderRadius 5 :border "1px solid black"}
         :body {:margin 0 :padding 0 :fontFamily "Ubuntu, sans-serif"}
         :div {:margin 0 :padding 0} }))

(route "style"
       #(go (clj->js {:http-headers {:Content-Type "text/css"}
                      :content (clj->css @default-style)})))
;; # Subscriptions
(register-sub :pid (fn [db _] (reaction (:pid @db))))
get-in
(register-sub :view-dimensions 
              (fn [db _] (reaction 
                           [(get-in @db [:viewport :width])
                            (get-in @db [:viewport :height])])))
(register-sub :width (fn [db _] (reaction (get-in @db [:viewport :width]))))    
(register-sub :height (fn [db _] (reaction (get-in @db [:viewport :height]))))    
(register-sub :app (fn [db _] (reaction (first (:path @db)))))
(register-sub :render-html5 (fn [db _] (reaction true)))
(register-sub 'db (fn [db _] (reaction @db)))
#_(js/console.log (clj->js @(subscribe ['db]))) ; debug
;; # Event handler
(register-handler 
  :route 
  (fn [db [route] _] 
    (into db route)))
(register-handler 
  :update-viewport
  (fn [db _ _]
    (-> db (assoc-in [:viewport :width] js/window.innerWidth)
        (assoc-in [:viewport :height] js/window.innerHeight))))
;; # Components
(defn style [o]
  [:style {"dangerouslySetInnerHTML"
           #js {:__html (clj->css o)}}])
;; # App
;; ## Notes
;;
;; We try to follow [iOS Human Interface Guidelines](
;; https://developer.apple.com/library/ios/documentation/UserExperience/Conceptual/MobileHIG/),
;; but with a crossplatform focus. Secondary we accomodate
;; [Android Material Design](http://developer.android.com/design/) where possible.
;; The iOS guidelines are required get into the apple app-store.
;;
;; Common patterns are abstract, such that they can be implemented in a native way
;; on different platforms.
;;
;; We keep a database of creative commons icons, which are used within the app
;; (when no platform icon is available). https://thenounproject.com/ is good source for this,
;; though check that the icon follows the design guidelines, and include the license info
;; when loading it into the database.
;;
;; Data-notes
;; - `:type` - the type of the application
;;   - `:static` - static html or data, generated in parallel
;;   - `:html5` - standard html5 app
;;   - `:nwjs` - nwjs for daemons and desktop apps
;;   - `:cordova` - LATER additional apis available
;;   - `:extension` - LATER browser extension mozilla/chrom/opera, WebExtensions API
;;   - `:ios` - LATER react-native
;;   - `:android` - LATER react-native
;; - viewport
;;   - `:title` view title
;;   - `:navigate-back` back-button with `:event` and optional `:title`
;;   - `:actions` sequence view-specific actions with `:icon`, `:title`, `:active` and `:event`,
;;     similar to iOS Toolbar or Android Actions
;;   - `:views ` sequence of views with `:icon`, `:title`, `:active` and `:event`,
;;     similar to iOS Tabbar or Android Navigation
;;   - `:width` `:height` width and height of the viewport including bars
;;   - `:scrollX` `:scrollY` scroll position within the viewport
;;   - `:transition` NOT IMPLEMENTED
;;   - `:style` stylesheet as map of style maps
;;   - `:done` static application content is ready to be send, - defaults to true
;; - `:pid` - an id of the current process, - this is the target of an event dispatch
;;

;; ## Actual app component
(def bar-height 30)
(def bar-shadow "0px 1px 4px rgba(0,0,0,.4)")
(def bar-color "rgba(255,244,233,0.9)")
(defn icon [id]
  [:span "[" id "]"])
(def app-style ; ###
  (ratom/reaction
    {:h1 {:background "red"}
     :.solsort-app {:background :blue}
     :.float-right {:float :right}
     :.float-left {:float :left}

     :.bar 
     {:width "100%"
      :text-align :center
      :display :inline-block
      :background bar-color
      :box-shadow bar-shadow
      :line-height bar-height  
      :height bar-height
      :position :fixed  }

     :.topheight {}
     :.barheight {:height bar-height}
     :.botbar { :top  (- @(subscribe [:height]) bar-height) }}))
(defn app [o] ; ###
  (let [title (:title o)
        navigate-back (:navigate-back o)
        actions (:actions o)
        views (:views o)
        content (:html o) ]
    (aset js/document "title" title)
    [:div {:style {:position "absolute"
                   :width "100%" }}
     (solsort.core/style @app-style)

     [:div.topbar.bar 
      [:span.middle title]
      (when navigate-back
        [:span.float-left 
         {:on-click #(dispatch (:event navigate-back))} 
         [icon (:icon navigate-back)]
         " " (:title navigate-back)])

      (when actions
        (into 
          [:span.float-right]
          (map 
            (fn [a] [:span.barbutton 
                  {:on-click #(dispatch (:event a))}
                  " " [icon (:icon a)] " "])
            actions)))]
     (when views
        (into 
      [:div.botbar.bar]
          (map 
            (fn [a] [:span.barbutton 
                  {:on-click #(dispatch (:event a))}
                  " " [icon (:icon a)] " "])
            views)))
    [:div.barheight]
     [:h1 title]
     [:div (str @(subscribe [:view-dimensions]))]
     content
     (when views [:div.barheight])]))

;; ## Sample app
(route 
  "hello" 
  (fn []  
    (atom {:type :app
     :title "Hello-app"
     :navigate-back {:event ['home] :title "Home" :icon "home"}
     :actions [ {:event ['copy] :icon "copy"}
               {:event ['paste] :icon "paste"} ]
     :views [ {:event ['view-left] :icon "left"}
             {:event ['view-right] :icon "right"} ]
     :html
     [:div "hi" (str (range 1000))]})))
