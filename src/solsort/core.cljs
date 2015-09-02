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
    [re-frame.core :as re-frame]
    [goog.net.Jsonp]
    [goog.net.XhrIo]
    [reagent.core :as reagent :refer  []]))

(enable-console-print!)

(def is-figwheel (some? js/window.figwheel))
(when is-figwheel (js/setTimeout #(run-tests) 0))

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

;; remove this:
(defonce state
  (reagent/atom
    {:path ["lemon"]
     :args {}
     :viewport
     {:width js/window.innerWidth
      :height js/window.innerHeight }}))

;; # logger
(defn log [& args] (apply print 'log args))

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

(defn get-route-fn [path]
  (or  (@routes (first path)) (:default @routes) #{}))

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
    (when route
      ((get-route-fn (:path route))))))

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

;; ## async channels
(defn chan? [c] (instance? ManyToManyChannel c))

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

;; ## functions
(defn run-once [f]
  (let [do-run (atom true)]
    (fn [& args]
      (when @do-run
        (reset! do-run false)
        (apply f args)))))

;; # Components
(defn style [o]
  [:style {"dangerouslySetInnerHTML"
           #js {:__html (clj->css o)}}])
;; # App
;; ## Design
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
;; ## App-state subscriptions
;;
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
(re-frame/register-sub
  :pid (fn [db _] (reaction (:pid @db))))
;; ## Event handlers
(register-handler
  :set-title
  (fn [db event dispatch]
    (assoc db :title (first event))
    )
  )
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
  (let [pid (:pid db)]
    (fn [event-id & args]
      (apply re-frame/dispatch event-id pid args))))

(defn register-handler
  ; TODO and middleware removing pid, and optionally swapping db
  ([event-id f]
   (re-frame/register-handler event-id
                              (fn [db event] (f db event (-create-dispatch-fn db)))))
  ([event-id middleware f]
   (re-frame/register-handler event-id middleware
                              (fn [db event] (f db event (-create-dispatch-fn db)))))
  )
(defn dispatch [event-id & args]
  (apply re-frame/dispatch event-id @(re-frame/subscribe [:pid]) args))

;; ## Appstyle
;; ## Actual app component
;;
(defn app [& {:keys [title navigate-back actions views content]
              :or {title "solsort" }}]
  (aset js/document "title" title)
  [:h1 "hello app"])

