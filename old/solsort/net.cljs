(ns solsort.net
  (:require-macros
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [reagent.ratom :refer-macros [reaction]]
    [reagent.core :as reagent]
    [goog.net.XhrIo]
    [clojure.string :as string]
    [solsort.misc :refer [log unique-id <exec <p]]
    [solsort.router :refer [route-exists? route]]
    [solsort.style :refer [default-style-str]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]
    [re-frame.core :refer [register-handler register-sub]]))

(register-sub :online (fn [db _] (reaction (:online @db))))
(register-handler :connect (fn [db [] _] (assoc db :online true)))
(register-handler :disconnect (fn [db [] _] (assoc db :online false)))
(defonce port 
  (if js/window.process
    (js/parseInt (or (-> js/process .-env (aget "PORT")) "4321") 10)
    4321))  

(defn <load-js 
  "Load a JavaScript file, and emit true on returned channel when done"
  [url]
  (let [c (chan)
        elem (js/document.createElement "script")]
    (js/document.head.appendChild elem)
    (aset elem "onload" (fn [] (put! c true)))
    (aset elem "src" url)
    c))

;; ## Util
(defn utf16->utf8 [s] (js/unescape (js/encodeURIComponent s)))
(defn utf8->utf16 [s] (js/decodeURIComponent(js/escape s)))
(defn buf->utf8-str [a] (string/join (map #(js/String.fromCharCode %) (seq (js/Array.prototype.slice.call (js/Uint8Array. a))))))
(defn buf->str [a] (utf8->utf16 (buf->utf8-str a)))
(defn utf8-str->buf [s] (js/Uint8Array.from (clj->js (map #(.charCodeAt % 0) s))))
(defn str->buf [s] (utf8-str->buf (utf16->utf8 s)))
;; ## Crypto - <sha256-str
(def browser-crypto (atom false))
(defn <sha256 [buffer]
  (go
    (when-not @browser-crypto
      ; check if browser-crypt exists/works or else load https://solsort.com/polycrypt.js
      ; reset! browser-crypto crypto.subtle || msCrypto.subtle || polycrypt
      (reset!
        browser-crypto
        (or (aget 
              (or js/window.crypto 
                  js/window.msCrypto 
                  #js{})
              "subtle")
            (do (<! (<load-js "https://solsort.com/js/polycrypt.js"))
                js/polycrypt))))
    (<! (<p (.digest @browser-crypto "SHA-256" buffer)))))
(defn <sha256-str [s] 
  (go (js/btoa (buf->utf8-str (<! (<sha256  (str->buf s)))))))

;; # mubackend
;; ## socket.io

(defonce client-rooms (atom {}))
(defonce client-connections (atom {}))
(defonce rooms (atom {}))

(defn mubackend-socket [socket]
  (let [client-id (.-id socket)]
    (log 'connect client-id)
    (.on socket "join"
         (fn [secret] 
           (go
             (let [room-id (<! (<sha256-str secret))]
               (swap! client-rooms #(assoc % client-id (conj (get % client-id #{}) room-id)))
               (swap! rooms #(assoc % room-id (conj (get % room-id #{}) client-id)))
               (swap! client-connections assoc client-id socket)
               ))))
    (.on socket "msg"
         (fn [msg] 
           (log "msg1" msg )
           (log "msg2" (aget msg 0) )
           (log "msg" (rand-nth (seq (get @rooms (aget msg 0) []))) )
           (let [dst (rand-nth (get rooms (first msg) []))]
             )))
    (.on socket "disconnect"
         (fn []
           (doall (for [room-id (@client-rooms client-id)]
                    (swap! rooms
                           #(let [room (% room-id)
                                  room (disj room client-id)]
                              (if (empty? room)
                                (dissoc @rooms room-id)
                                (assoc @rooms room-id room))))))
           (swap! client-rooms dissoc client-id)
           (swap! client-connections dissoc client-id)
           (log "disconnect" client-id))))
  )

;; # Server
(when (and (some? js/window.require)
           (some? (js/window.require "express")))
  (log "Running server")
  ;; ## old server
  (defonce old-clients (atom {}))
  (defonce daemons (atom {}))

  (defonce app ((js/require "express")))
  (defonce proxy (js/require "express-http-proxy"))
  (defonce server (.Server (js/require "http") app))
  (defonce io ((js/require "socket.io") server))
  (defonce p2p-server (aget (js/require "socket.io-p2p-server") "Server")) 


  (def request (js/require "request"))
  (defn auth-token [cookies] (second (re-find #"AuthSession=([^;]*)" (or cookies ""))))
  (defn new-socket-connection [socket]
    (mubackend-socket socket)
    (let [cookie (-> socket (.-handshake) (.-headers) (.-cookie))
          auth (auth-token cookie)
          id (.-id socket)]
      (request
        #js {:url (str "http://localhost:" port "/db/_session")
             :headers #js {:cookie cookie}}
        (fn [_ _ data]
          (log 'connect-auth data)
          (let [user (try (-> data
                              (js/JSON.parse)
                              (aget "userCtx")
                              (aget "name"))
                          (catch :default e nil))]
            (when (= "daemon" user) (swap! daemons assoc auth id))
            (swap! old-clients assoc id {:id id :user user :auth auth :socket socket})
            (log 'daemons @daemons)
            (log 'client-connect (@old-clients id))))))
    (.on socket "disconnect"
         (fn []
           (let [id (.-id socket) 
                 client (@old-clients id)]
             (log 'disconnect id)
             (swap! daemons dissoc (:auth client))
             (swap! old-clients dissoc id)))))

  (defn html->http [route data]
    (let [title (:title data)
          inner-html (reagent/render-to-string (:html data))
          route-str (js/JSON.stringify (clj->js route))
          ]
      {:type "text/html"
       :content  
       (str
         "<!DOCTYPE html><html><head>"
         "<script>"
         "solsort_route=" route-str  ";"
         "document.write('<script async src=\"//solsort.com/"
         "solsort.js\"></sc' + 'ript>');"
         "</script>"
         "<meta charset=\"UTF-8\">"
         ; TODO: make zoom optional
         "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,"
         "maximum-scale=1,user-scaleable=no\">"
         "<meta name=\"apple-mobile-web-app-capable\" content=\"yes\">"
         "<meta name=\"mobile-web-app-capable\" content=\"yes\">"
         "<title>" (:title data) "</title>"
         "<style>" (default-style-str) "</style>"
         "</head><body>"
         inner-html
         "</body></html>"
         )
       }))
  (defn http-result [res route data]
    (let [data (if (= :html (:type data))
                 (html->http route data)
                 data)]
      (if (= :json (:type data))
        (-> res
            (.jsonp (:json data))
            (.end))
        (if (string? (:type data))
          (-> res
              (.set "Content-Type" (:type data))
              (.end (:content data)))
          (.end res (str "unexpected :type " (:type data) " for " route))))))

  (defn middleware [req res cb]
    (let [route (solsort.router/url->route (aget req "url"))]
      (.header res "Access-Control-Allow-Origin" (or (-> req (aget "headers") (aget "origin")) "*"))
      (.header res "Access-Control-Allow-Credentials" "true")
      (.header res "Access-Control-Allow-Headers" "Content-Type")
      (.removeHeader res "X-Powered-By")
      (if (route-exists? (route "route")) 
        (go (http-result res route (<! (solsort.router/<extract-route route))))
        (cb))))

  (defonce prev-middleware (atom))
  (defn remove-middleware [f]
    (let [stack (-> app (aget "_router") (aget "stack"))]
      (loop [i 0]
        (when (< i (.-length stack))
          (log 'remove-middleware i)
          (if (= @prev-middleware (aget (aget stack i) "handle"))
            (aset (aget app "_router") "stack" (.concat (.slice stack 0 i) (.slice stack (inc i))))
            (recur (inc i)))))))
 

(defn add-middleware []
  (log 'add-middleware)
  (when (some? @prev-middleware) (remove-middleware @prev-middleware))
  (reset! prev-middleware middleware)
  (.use app middleware)

  (defonce start-server
    (do
      (log 'starting-server port)
      (.use io p2p-server)
      (.use app "/es" 
            (proxy "localhost:9200" 
              #js {"limit" "256mb"
                   "forwardPath" 
                   (fn [req res] 
                     (when-not (= "GET" (aget req "method"))
                       (js/console.log req)
                        (throw "Only GET allowed for ElasticSearch"))
                     (.header res "Access-Control-Allow-Origin" (or (-> req (aget "headers") (aget "origin")) "*"))
                     (.header res "Access-Control-Allow-Credentials" "true")
                     (.header res "Access-Control-Allow-Headers" "Content-Type")
                     (aget req "url"))
                   ;"intercept"
                   ;(fn [rsp data req res callback] 
                   ;(.header res "Access-Control-Allow-Origin" (or (-> req (aget "headers") (aget "origin")) "*"))
                   ;(.header res "Access-Control-Allow-Credentials" "true")
                   ;(.header res "Access-Control-Allow-Headers" "Content-Type")
                   ;  (callback nil data))
                   }))
      (.use app "/db" 
            (proxy "localhost:5984" 
              #js {"limit" "256mb"
                   "forwardPath" 
                   (fn [req res] 
                     (.header res "Access-Control-Allow-Origin" (or (-> req (aget "headers") (aget "origin")) "*"))
                     (.header res "Access-Control-Allow-Credentials" "true")
                     (.header res "Access-Control-Allow-Headers" "Content-Type")
                     (aget req "url"))
                   ;"intercept"
                   ;(fn [rsp data req res callback] 
                   ;(.header res "Access-Control-Allow-Origin" (or (-> req (aget "headers") (aget "origin")) "*"))
                   ;(.header res "Access-Control-Allow-Credentials" "true")
                   ;(.header res "Access-Control-Allow-Headers" "Content-Type")
                   ;  (callback nil data))
                   }))
      (.on io "connection" #(new-socket-connection %))
      (.listen server port)
      (log "started server")
      nil)))

(add-middleware))

;; # Client connection
(def is-dev (or (= "file:" js/location.protocol)
                (re-find #"localhost" js/location.hostname)
                (contains? #{"3449" "3000"} js/location.port)))
(def location-hostname (if (= "" js/location.hostname) "localhost" js/location.hostname))
#_(def host (if is-dev 
            (str "http://" location-hostname ":" port "/")
            (str "https://blog.solsort.com/")))
(def host "https://solsort.com/")

; TODO keep track on connected rooms, and rejoin on reconnect
(def socket (atom false))
(defn socket-emit [a b]
  (go
    (log 'socket-emit a b)
    (when-not @socket
      (when-not (some? js/window.io)
        (<! (<load-js (str host "socket.io/socket.io.js"))))
      (when-not @socket (reset! socket (js/io host))))
    (.emit @socket a b)))

;; # msg api
;; ## Actual api
(defn msg 
  ([realm mbox rrealm rmbox content] (socket-emit "msg" #js[realm mbox rrealm rmbox content]))
  ([realm mbox content] (msg realm mbox nil nil content)))
(defn join 
  "Connect to a realm. The realm-id is the hash of the secret, and is returned"
  [secret]
  (socket-emit "join" secret)
  )
(defn leave [realm])
(defn handler [f])
;; # <ajax
(defn <ajax [url & {:keys [method data headers timeout credentials result]
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
      method data (clj->js headers) timeout credentials)
    c))
;; # autorestart
#_(when (and 
          false
          (some? js/window.require)
          (some? (js/window.require "fs")))
    (let [fs (js/require "fs")
          script-file  "/solsort/html/solsort.js"  
          ]
      (when (.existsSync fs script-file)
        (.watchFile fs script-file
                    (fn []
                      (log 'XXXXX 'restarting-changed script-file)
                      (go (<! (timeout 10000))
                          (js/process.exit)))))))

(route "cors" ;; #
       (fn [o]
         (go
           (let [secure (= (.slice (o "path") 0 10) "cors/https") ; http or https (nothing else allowed)
                 url-part (.slice (o "path") (if secure 11 10))
                 url-part (re-matches #"^[^:]*/.*" url-part) ; disallow other than default-port
                 url (str "http" (if secure "s" "") "://" url-part)
                 ]
             (log secure o url-part url)
            {:type "text/plain"
            :content (<! (<ajax url :result "text"))}))))
;; # Experiments

#_
((join "blah")
 (go
   (let [realm (<! (<sha256-str "blah"))]
     (msg realm "hello" "blah")

     )

   )
 (socket-emit "msg" "hello123")
 (msg "foo" "bar" "baz"))
