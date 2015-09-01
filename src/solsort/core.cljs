;; # core / definition / dependencies
(ns solsort.core
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is]]
    [clojure.string :as string :refer  [split]]
    [clojure.string :refer  [join]]
    [cognitect.transit :as transit]
    [goog.net.Jsonp]
    [goog.net.XhrIo]
    [reagent.core :as reagent :refer  []]))

(enable-console-print!)

;; # re-frame

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
  (log 'here @state)
  (log (<! (ajax "http://localhost:1234/db/")))
  (log (<! (ajax "http://localhost:1234/db/_session"
                 :method "POST"
                 :data {:name (@gargs "user") :password (@gargs "password")}
                 )))
  (log (type nil))
  )

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

#_(testcase 'clj->css
            #(= (clj->css {:h1 {:fontWeight :normal :fontSize 14} :.div {:background :blue}})
                "h1{font-weight:normal;font-size:14px}.div{background:blue}"))

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
; # util
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
  (.replace (.trim (.toLowerCase s)) (js/RegExp. "(%[0-9a-fA-F][0-9a-fA-F]|[^a-z0-9])+", "g") "-"))
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
