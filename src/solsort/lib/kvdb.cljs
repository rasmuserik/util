(ns solsort.lib.kvdb
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]])
  (:require
    [solsort.lib.old-kvdb :as old-kvdb]
    [solsort.sys.platform :refer [is-nodejs is-browser ensure-dir]]
    [solsort.sys.mbox :refer [route log]]
    [cljs.reader :refer [read-string]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))



(def dbs (atom {}))


(when is-nodejs
  (defn open-db [db]
    (go
      (ensure-dir "./dbs")
      (swap! dbs assoc db ((js/require "levelup")
                           (str "./dbs/kvdb-" (.replace (str db) #"[^a-zA-Z0-9]" "_") ".leveldb")
                           #js{"valueEncoding" "json"}))))
  (defn execute-transaction [queries stores]
    (let [c (chan)
          writes-left (atom (count stores))
          ]
      (when (zero? (count stores)) (close! c))
      (doall
        (for [query (seq stores)]
          (let [db-name (first query)
                db (get @dbs db-name)
                kvs (second query)]
            (.batch db
                    (clj->js (for [[k v] (seq kvs)] {:type "put" :key k :value v}))
                    (fn [err]
                      (when err (log 'kvdb 'get 'error err))
                      (when (zero? (swap! writes-left dec)) (close! c)))))))
      (doall
        (for [query queries]
          (let [db-name (first query)
                db (get @dbs db-name)
                kvs (second query)]
            (doall
              (for [[k listeners] (seq kvs)]
                (.get db k
                      (fn [err result]
                        (when (and err (not= (aget err "type") "NotFoundError"))
                          (log 'kvdb 'get 'error err))
                        (doall
                          (for [listener listeners]
                            (if result
                              (put! listener result)
                              (close! listener)))))))))))


      c)))


(when is-browser
  (def indexed-db (atom nil))
  (defn open-db [db]
    (if @indexed-db (.close @indexed-db))
    (let [c (chan)
          store-list (conj (set (read-string (or (.getItem js/localStorage "kvdbs") ""))) db)
          req (.open js/indexedDB "kvdb" (inc (count store-list)))]
      (reset! dbs store-list)
      (.setItem js/localStorage "kvdbs" (str store-list))
      (set! (.-onupgradeneeded req)
            (fn [req]
              (let [db (.-result (.-target req))]
                (doall (for [store store-list]
                         (if (not (.contains (.-objectStoreNames db) store))
                           (.createObjectStore db store)))))))
      (set! (.-onerror req)
            (fn [err]
              (log 'kvdb 'upgrade-error)
              (js/console.log 'error err)))
      (set! (.-onsuccess req)
            (fn [req]
              (reset! indexed-db (.-result (.-target req)))
              (close! c)))
      c))
  (defn execute-transaction [queries stores]
    (let [c (chan)
          read-only (zero? (count stores))
          dbs (into (set (keys queries)) (keys stores))
          transaction (.transaction @indexed-db
                                    (clj->js (seq dbs))
                                    (if read-only "readonly" "readwrite"))
          ]
      (doall
        (for [query stores]
          (let [db (first query)
                kvs (second query)
                object-store (.objectStore transaction db)]
            (doall
              (for [[k v] (seq kvs)]
                (let [req (.put object-store v k)]
                  (aset req "onabort"
                        (fn []
                          (log 'kvdb 'put-abort db k v)))
                  (aset req "onerror"
                        (fn []
                          (log 'kvdb 'put-error db k v)))
                  ))))))
      (doall
        (for [query queries]
          (let [db (first query)
                kvs (second query)
                object-store (.objectStore transaction db)]
            (doall
              (for [[k listeners] (seq kvs)]
                (let [req (.get object-store k)]
                  (aset req "onsuccess"
                        (fn []
                          (let [result (.-result req)]
                            (doall
                              (for [listener listeners]
                                (if result
                                  (put! listener result)
                                  (close! listener)))))))))))))
      (go (<! (timeout 0))))))


(declare commit)
(def cache (atom {})) ; stores enqueuede
(def store-count (atom 0)) ; number of unexecuted stores
(def queries (atom {}))
(def transaction-listeners (atom []))

(def prev-cache (atom {})) ; stores currently being executed
(def transaction-request (chan 1))

(defn run-transaction [queries stores]
  (go
    (loop [db-list (seq (into (set (keys queries)) (keys stores)))]
      (when (first db-list)
        (when-not (contains? @dbs (first db-list))
          (<! (open-db (first db-list))))
        (recur (rest db-list))))
    (when (pos? (+ (count queries) (count stores)))
      (<! (execute-transaction queries stores)))))

(defn transaction-loop []
  (go
    (loop []
      (<! transaction-request)
      ; NB: not thread safe
      (let [listeners @transaction-listeners
            qs @queries
            stores @cache]
        (reset! prev-cache @cache)
        (reset! cache {})
        (reset! store-count 0)
        (reset! queries {})
        (reset! transaction-listeners [])
        (<! (run-transaction qs stores))
        (loop [listeners listeners]
          (when (first listeners)
            (put! (first listeners) true)
            (recur (rest listeners))))
        (recur)))))
(transaction-loop)

(defn transact [] (put! transaction-request true))
(defn db-fetch [db k]
  (let [c (chan 1)]
    (swap! queries
           assoc-in [db k]
           (conj (get-in @queries [db k] '()) c))
    (transact)
    c))


(defn store [db k v]
  (let [db (str db)
        k (str k)]
    (swap! cache assoc-in [db k] v)
    (when (= @store-count 0) (transact))
    (swap! store-count inc)
    (if (< @store-count 1000) (go) (commit))))

(defn fetch [db k]
  (let [db (str db)
        k (str k)]
    (go (or (get-in @cache [db k])
            (get-in @prev-cache [db k])
            (<! (db-fetch db k))))))

(defn commit []
  (let [c (chan 1)]
    (swap! transaction-listeners conj c)
    (transact)
    c))


(defn time-async [text f & args]
  (go
    (let [t0 (js/Date.now)
          result (<! (apply f args))
          t (- (js/Date.now) t0)]
      (log 'time-async text t)
      result)))

(defn bench []
  (go
    (<! (time-async
          "writes"
          #(go
             (loop [i 10000]
               (<! (store 'kvdb-bench (str i) i))
               (when (pos? i)
                 (recur (dec i))))
             (<! (commit)))))
    (<! (time-async
          "reads"
          #(go
             (log 'kvdb-bench 'sum
                  (loop [i 1000 sum 0]
                    (when (pos? i)
                      (recur (dec i) (+ sum (<! (fetch 'kvdb-bench (str i))))))))
             )))
    ))
(route "kvdb"
       #(go
          (log 'kvdb 'test-start)
          (log 'kvdb 'ab0 (<! (fetch "a" 'b)))
          (log 'kvdb 'ab0 (.-constructor (<! (fetch "a" 'b))))
          (fetch "a" "b")
          (fetch "a" "b")
          (store "foo" :bar :baz)
          (store "foo" :quu :baz)
          (store "foo" :bla :baz)
          (store 'a 'b "hello")
          (log 'kvdb 'ab1 (<! (fetch "a" 'b)))
          (store "foo" :quu nil)
          (store 'a 'b (js/ArrayBuffer. 20))
          (log 'kvdb-queries queries)
          (log 'kvdb-cache cache)
          (<! (bench))
          ))


;; Generic functions
(defn store-channel [db c]
  (go-loop
    [key-val (<! c)]
    (if key-val
      (let [[k v] key-val]
        (<! (store db k (clj->js v)))
        (recur (<! c)))
      (<! (commit)))))

(defn multifetch [storage ids]
  (let [c (chan 1)
        result #js{}
        cnt (atom (count ids))]
    (if (zero? @cnt)
      (close! c)
      (doall (for [id ids]
               (take! (fetch storage id)
                      (fn [value]
                        (aset result id value)
                        (if (<= (swap! cnt dec) 0)
                          (put! c (js->clj result))))))))
    c))
