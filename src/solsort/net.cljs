(ns solsort.net
  (:require-macros
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [reagent.ratom :refer-macros [reaction]]
    [goog.net.XhrIo]
    [solsort.misc :refer [log unique-id]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]
    [re-frame.core :refer [register-handler register-sub]]))

(register-sub :online (fn [db _] (reaction (:online @db))))
(register-handler :connect (fn [db [] _] (assoc db :online true)))
(register-handler :disconnect (fn [db [] _] (assoc db :online false)))
;; # Server
(when (and (some? js/window.require)
           (some? (js/window.require "express")))
  (log "Running server")

  (defonce clients (atom {}))
  (defonce daemons (atom {}))

  (defonce app ((js/require "express")))
  (defonce proxy (js/require "express-http-proxy"))
  (defonce server (.Server (js/require "http") app))
  (defonce io ((js/require "socket.io") server))
  (defonce p2p-server (aget (js/require "socket.io-p2p-server") "Server")) 


  (def request (js/require "request"))
  (defn auth-token [cookies] (second (re-find #"AuthSession=([^;]*)" (or cookies ""))))
  (defn new-socket-connection [socket]
    (let [
          cookie (-> socket (.-handshake) (.-headers) (.-cookie))
          auth (auth-token cookie)
          id (.-id socket)]
      (request
        #js {:url "http://localhost:1234/db/_session"
             :headers #js {:cookie cookie}}
        (fn [_ _ data]
          (log 'connect-auth data)
          (let [user (try (-> data
                              (js/JSON.parse)
                              (aget "userCtx")
                              (aget "name"))
                          (catch :default e nil))]
            (when (= "daemon" user) (swap! daemons assoc auth id))
            (swap! clients assoc id {:id id :user user :auth auth :socket socket})
            (log 'daemons @daemons)
            (log 'client-connect (@clients id))))))
    (.on socket "disconnect"
         (fn []
           (let [id (.-id socket) 
                 client (@clients id)]
             (log 'disconnect id)
             (swap! daemons dissoc (:auth client))
             (swap! clients dissoc id)))))

  (defonce start-server
    (do
      (log 'starting-server)
      (.use io p2p-server)
      (.use app "/db" (proxy "localhost:5984" #js {"forwardPath" #(aget % "url")}))
      (.on io "connection" #(new-socket-connection %))
      (.listen server 1234)
      (log "started server")
      nil)))


;; # Client connection
(def is-dev (or
    (= "file:" js/location.protocol)
    (contains? #{"3449" "3000"} js/location.port)))
(def location-hostname (if (= "" js/location.hostname) "localhost" js/location.hostname))
(def host (if is-dev 
                   (str "http://" location-hostname ":1234/")
                   (str js/location.protocol "//blog.solsort.com/")))
(def socket-path (str host "socket.io/"))

(defn load-js 
  "Load a JavaScript file, and emit true on returned channel when done"
  [url]
  (let [c (chan)
        elem (js/document.createElement "script")]
    (js/document.head.appendChild elem)
    (aset elem "onload" (fn [] (put! c true)))
    (aset elem "src" url)
    c))

(defn socket-connect []
  (go
    (when-not (some? js/window.io)
        (<! (load-js (str socket-path "socket.io.js"))))
    ))
(socket-connect)
;; # Network api
; TODO
(defn sub
  ([chan-id subscribe-key] (chan)) ; TODO
  ([chan-id] (sub chan-id nil))) 
(defn id [] (unique-id)) ; might be optimised for routing later on
(defn pub! [chan-id msg] ) ; put! to named channel, goes to an arbitrary subscriber

;; ## Experiments
(when js/window.socket
  (js/socket.removeAllListeners "http-request")
  (js/socket.removeAllListeners "http-response-log")
  (js/socket.removeAllListeners "socket-connect")
  (js/socket.removeAllListeners "socket-disconnect")
  (js/socket.on
    "http-request"
    (fn [o] (log "http-request" o)
      (js/socket.emit
        "http-response"
        #js {:url (aget o "url")
             :key (aget o "key")
             :content (str "Hello " (aget o "url"))})
      ))
  (js/socket.on
    "http-response-log"
    (fn [o] (log "http-response" o)))
  (js/socket.on
    "socket-connect"
    (fn [o] (log "connect" o)))
  (js/socket.on
    "socket-disconnect"
    (fn [o] (log "discon" o)))

  (js/p2p.on
    "ready"
    ;(aset js/p2p "usePeerConnection" true)
    (log 'p2p-ready)
    ;(js/p2p.emit "hello" #js {:peerId js/navigator.userAgent})
    )

  (js/p2p.removeAllListeners "hello")
  (js/p2p.on
    "hello"
    (fn [o] (log o))
    )

  (go (loop [i 0]
        (<! (timeout 5000))
        (js/p2p.emit "hello" (clj->js [i (str js/navigator.userAgent)]))
        (when (< i 3) (recur (inc i))))))

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
