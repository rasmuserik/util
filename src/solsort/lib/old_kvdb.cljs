(ns solsort.lib.old-kvdb
  (:require-macros [cljs.core.async.macros :refer [go alt! go-loop]])
  (:require
    [solsort.sys.test :refer [testcase]]
    [solsort.sys.platform :refer [is-browser ensure-dir]]
    [cljs.reader :refer [read-string]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))


;; IndexedDB implementation
(if is-browser
  (do
    (def stores (atom {}))
    (def db (atom nil))
    (def locked (atom false))
    (defn lock [id]
      (go
        ;  (print 'locking id)
        (while @locked
          (<! (timeout 100)))
        ;  (print 'lock id)
        (reset! locked true)))
    (defn unlock [id]
      ; (print 'unlock id)
      (reset! locked false))
    (defn open-db []
      (go
        (if @db (.close @db))
        (<! (lock 'a))
        (let [c (chan)
              store-list (seq (read-string (.getItem js/localStorage "keyval-db")))
              req (.open js/indexedDB "keyval-db" (inc (count store-list)))
              ]
          (set! (.-onupgradeneeded req)
                (fn [req]
                  (print 'upgrade-needed-start)
                  (let [db (.-result (.-target req))]
                    (doall (for [store store-list]
                             (if (not (.contains (.-objectStoreNames db) store))
                               (.createObjectStore db store)))))))
          (set! (.-onerror req) #(do
                                   (unlock 'a1)
                                   (js/console.log 'error %)))
          (set! (.-onsuccess req)
                (fn [req]
                  (unlock 'a2)
                  (reset! db (.-result (.-target req)))
                  (close! c)))
          (<! c))))

    (defn ensure-store [storage]
      (go
        (if (not (@stores storage))
          (let [store-list (read-string (or (.getItem js/localStorage "keyval-db") "#{}"))]
            (swap! stores assoc storage {})
            (.setItem js/localStorage "keyval-db" (str (conj store-list storage)))
            (<! (open-db)))
          (while (not @db) (<! (timeout 100))))))

    (defn commit [storage]
      (go

        (if (< 0 (count (@stores storage)))
          (do
            (<! (lock 'b))
            (let [c (chan 1)
                  trans (.transaction @db #js[storage] "readwrite")
                  objStore (.objectStore trans storage)]
              (doall (for [[k v] (@stores storage)]
                       (.put objStore v k)))
              (set! (.-oncomplete trans)
                    #(do (unlock 'b1)
                         (put! c true)))
              (set! (.-onerror trans)
                    #(do
                       (unlock 'b2)
                       (print "commit error")
                       (close! c)))
              (set! (.-onabort trans)
                    #(do
                       (unlock 'b3)
                       (print "commit abort")
                       (close! c)))
              (swap! stores assoc storage {})
              (<! c))))))

    (defn multifetch [storage ids]
      (go
        (<! (ensure-store storage))
        (<! (commit storage))
        (<! (lock 'c))
        (let [c (chan)
              result (atom #js{})
              transaction (.transaction @db #js[storage] "readonly")
              object-store (.objectStore transaction storage)]
          (doall (for [id ids]
                   (let [request (.get object-store id)]
                     (set! (.-onsuccess request)
                           (fn [] (aset @result id (.-result request)))))))
          (set! (.-oncomplete transaction)
                (fn []
                  (put! c @result)))
          (let [return-value (<! c)]
            (unlock 'c)
            return-value)
          )))

    (defn fetch [storage id]
      (go
        (aget (or (<! (multifetch storage #js[id])) #js{}) id)
        ))

(defn store [storage id value]
  (go
    (if (< 1000 (count (@stores storage))) (<! (commit storage)))
    (<! (ensure-store storage))
    (swap! stores assoc storage (assoc (@stores storage) id value))
    value))
(defn tryout []
  (go
    (print 'HERE (seq #js[1 2 3 4]))
    (<! (store :a "foo" "bar"))
    (<! (store :a "blah" "foop"))
    (<! (store :a "quux" "quuz"))
    (<! (store "b" "foo" "bar"))
    (<! (store "b" "bar" "baz"))
    (<! (store "b" "baz" "quux"))
    (print 'HERE2 (seq #js[1 2 3 4]))
    (print "stored")
    (print "A" (<! (fetch :a "blah")))
    (print "B" (<! (multifetch "b" #js["foo" "bar" "baz"])))))
)


;; LevelDB implementation
(do (comment leveldb)
    (def dbs (atom {}))
    (defn get-db [id]
      (or (get @dbs id)
          (do
            (ensure-dir "./dbs")
            (get
              (reset! dbs (assoc @dbs id
                                 ((js/require "levelup")
                                  (str "./dbs/" (.replace (str id) #"[^a-zA-Z0-9]" "_") ".leveldb")
                                  #js{"valueEncoding" "json"})))
              id))))

    (defn commit [storage] (go))
    (defn fetch [storage id]
      (let [c (chan 1)]
        (.get (get-db storage)
              id
              (fn [err value]
                (if err
                  (close! c)
                  (put! c value))))
        c))

    (defn multifetch [storage ids]
      (let [c (chan 1)
            result #js{}
            cnt (atom (count ids))]
        (if (= 0 @cnt)
          (close! c)
          (doall (for [id ids]
                   (take! (fetch storage id)
                          (fn [value]
                            (aset result id value)
                            (if (<= (swap! cnt dec) 0)
                              (put! c result)))))))
        c))

    (defn store [storage id value]
      (let [c (chan 1)]
        (.put (get-db storage)
              id
              value
              (fn [err]
                (if err (print 'leveldb-store-error err storage id value))
                (close! c)))
        c))
    ))


;; Generic functions
(defn store-channel [db c]
  (go-loop
    [key-val (<! c)]
    (if key-val
      (let [[k v] key-val]
        (<! (store db k (clj->js v)))
        (recur (<! c)))
      (<! (commit db)))))


;; Test
(testcase 'store
          #(go (or (<! (store :testdb "hello" "world")) true)))
(testcase 'fetch
          #(go (= "world" (<! (fetch :testdb "hello")))))
