(ns solsort.db
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [clojure.string :as string :refer  [split]]
    [re-frame.core :as re-frame :refer [register-sub subscribe register-handler dispatch dispatch-sync]]
    [solsort.util :as util :refer [function? chan? unique-id]]
    [solsort.net :as net :refer [ajax]]
    [reagent.core :as reagent :refer  []]))


;; # DB-login
#_(defonce gargs (atom {}))
#_(go
  (<! (ajax "http://localhost:1234/db/_session"
            :method "POST"
            :data {:name (@gargs "user") :password (@gargs "password")})))

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
