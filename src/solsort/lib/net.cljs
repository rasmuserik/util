(ns solsort.lib.net
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:require
    [solsort.sys.test :refer [testcase]]
    [solsort.sys.mbox :refer [post local msg log processes parent children handle]]
    [solsort.sys.platform :refer [is-nodejs is-browser set-immediate XHR global]]
    [solsort.sys.util :refer [unique-id]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))


;; WebSocket connections
(def pids (atom {}))
(defn broadcast [mbox data]
  (doseq [pid (keys @pids)]
    (post pid mbox data)))
(defn send-message [msg]
  (.send (@pids (aget msg "pid")) (js/JSON.stringify msg)))
(defn add-connection [pid ws]
  (swap! processes assoc pid send-message)
  (swap! pids assoc pid ws)
  (post local "connect" pid)
 ; (log 'ws 'added-connection pid @pids)
  )
(defn close-connection [id]
    ;(log 'ws id 'close)
    (swap! processes dissoc id)
    (swap! pids dissoc id)
    (post local "disconnect" id))
(defn handle-message [pid]
  (fn [msg]
    (let [msg (js/JSON.parse msg)]
      (aset msg "src" (str "ws:" pid))
      (post msg)
      (log 'ws pid 'msg msg))))


(when is-nodejs
  (def ws (js/require "ws"))
  (defn start-websocket-server [http-server]
    (log 'ws 'start)
    (let [ws (js/require "ws")
          wss (ws.Server. #js{:server http-server})]
      (.on wss "connection"
           (fn [ws]
             (log 'ws 'incoming-connection ws)
             (.send ws (js/JSON.stringify  #js{:pid local}))
             (.on ws "message"
                  (fn [data flags]
                    (let [data (js/JSON.parse data)
                          pid (aget data "pid") ]
                      (when pid
                        (swap! children conj pid)
                        (.removeAllListeners ws "message")
                        (.on ws "message" (handle-message pid))
                        (.on ws "close"
                             (fn []
                               (swap! children disj pid)
                               (close-connection pid)))
                        (add-connection pid ws))
                      (when-not pid (log 'ws 'error-unexpected-first-message data))))))))))


(when is-browser
  (comment keep-alive loop)
  (go
    (loop []
      (<! (timeout 55000))
      (broadcast "keep-alive" nil)
      (recur)))

  (def socket-server ;: url for websocket server
    (if (= -1 (.indexOf js/location.origin "solsort"))
      (if (= "http" (.slice js/location.origin 0 4))
        (str (.replace js/location.origin #"https?" "ws") "/ws/")
        "ws://ws.solsort.com/ws/")
      "ws://ws.solsort.com/ws/"
      ))

  (defn ws-connect []
    (log 'ws 'connect)
    (let
      [ws (js/WebSocket. socket-server)]
      (aset ws "onopen" (fn [e] (.send ws (js/JSON.stringify #js{:pid local}))))
      (aset ws "onerror" (fn [e] (log 'ws 'error) (js/console.log e)))
      (aset ws "onclose"
            (fn [e]
              (log 'ws 'close e)
              ; TODO exponential delay reconnect if server to connect to
              (go
                (<! (timeout 1000))
                (ws-connect))))
      (aset ws "onmessage"
            (fn [e]
              (log 'ws 'message)
              (let [data (js/JSON.parse (aget e "data"))
                    pid (aget data "pid")
                    message-handler (handle-message pid)]
                (if pid
                  (do
                    (aset ws "onmessage" (fn [e] (message-handler (aget e "data"))))
                    (aset ws "onclose"
                          (fn [e]
                            (close-connection pid)
                            (reset! parent nil)
                            (set-immediate ws-connect)))
                    (add-connection pid ws)
                    (reset! parent pid))
                  (log 'ws 'error-unexpected-first-message data)
                  ))))
      ))
  (set-immediate ws-connect))



;; http-client/AJAX
(defn ajax [url & {:keys [post-data CORS jsonp]}]
  (if (and jsonp is-browser)
    (let [url (str url "?callback=")
          c (chan)
          id (unique-id)]
      (aset global id
            (fn [o]
              (if o
                (put! c (js/JSON.stringify o))
                (close! c))
              (goog.object.remove global id)))
      (let [tag (js/document.createElement "script")]
        (aset tag "src" (str url id))
        (js/document.head.appendChild tag))
      c)
    (let [c (chan)
          req (XHR.)]
      (.open req (if post-data "POST" "GET") url true)
      (when CORS (aset req "withCredentials" true))
      (aset req "onreadystatechange"
            (fn []
              (when (= (aget req "readyState") (or (aget req "DONE") 4))
                (let [text (aget req "responseText")]
                  (if text
                    (put! c text)
                    (close! c))))))
      (.send req)
      c)))



; debug
(handle "connect" (fn [msg] (log 'connect msg)))
(handle "disconnect" (fn [msg] (log 'disconnect msg)))
