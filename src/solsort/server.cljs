(ns solsort.server
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [goog.net.XhrIo]
    [solsort.core :refer [ajax route log unique-id]]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

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


  (log (type @daemons))
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
