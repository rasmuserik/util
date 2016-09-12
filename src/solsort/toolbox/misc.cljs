(ns solsort.toolbox.misc
  (:require-macros
    [cljs.core.async.macros :refer  [go go-loop alt!]])
  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [clojure.string :as string :refer  [split join]]
    [goog.net.Jsonp]
    [goog.net.XhrIo]
    [reagent.core :as reagent :refer  []]))

(enable-console-print!)

(defn hex-byte [i] (.slice (.toString (bit-or i 256) 16) -2))
(defn async-err "wrap in an js/Error object, if not already an error" [e]
  (if (instance? js/Error e) e
      (let [o (js/Error. e)]
        (aset o "data" e)
        o)))
(defn throw-error "throw e if e is an js/Error instance. used by the <? macro" [e]
  (if (instance? js/Error e) (throw e) e))

(defn next-tick [f] (js/setTimeout f 0))
(defn unatom [o] (if (satisfies? IAtom o) @o o))
(defn put!close!  [c d]  (if  (nil? d)  (close! c)  (put! c d)))
(defn <p
  "Convert a javascript promise to a core.async channel"
  [p]
  (let  [c  (chan)]
    (.then p
           #(put!close! c %)
           #(put! c (async-err %)))
    c))

(defn <n
  "Convert a javascript node-style async to core.async channel"
  [f & args]
  (let  [c  (chan)]
    (apply f
           (conj args
                (fn [err res]
                    (if err
                      (put! c (async-err err))
                      (put!close! c res)))))
    c))

(defn <blob-url [blob]
  (let [reader (js/FileReader.)
        c (chan)]
    (aset reader "onloadend" #(put!close! c (aget reader "result")))
    (if blob
      (.readAsDataURL reader blob)
      (close! c))
    c))

(defn <blob-text [blob]
  (let [reader (js/FileReader.)
        c (chan)]
    (aset reader "onloadend" #(put!close! c (aget reader "result")))
    (if blob
      (.readAsText reader blob)
      (close! c))
    c))

(defn js-seq [o] (seq (js/Array.prototype.slice.call o)))
(defn starts-with [string prefix] (= prefix (.slice string 0 (.-length prefix))) )

(defn run-once [f]
  (let [do-run (atom true)]
    (fn [& args]
      (when @do-run
        (reset! do-run false)
        (apply f args)))))

(defn parse-json-or-nil [str]
  (try
    (js/JSON.parse str)
    (catch :default _ nil)))

(defn jsextend [target source]
  (let [ks (js/Object.keys source)]
    (while (pos? (.-length ks))
      (let [k (.pop ks)] (aset target k (aget source k)))))
  target)

(defn chan? [c] (instance? ManyToManyChannel c))
(defn function? [c] (instance? js/Function c))

(defn <seq<! [cs]
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
         (apply print (concat s (list 'done)))
         (xf result))
        ([result input]
         (swap! cnt inc)
         (when (< 60000 (- (.now js/Date) @prev-time))
           (reset! prev-time (.now js/Date))
           (apply print (concat s (list @cnt))))
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

(defn throttle "Limit how often a function (without arguments) is called"
  ([f t] (let [prev-t (atom 0)
               running (atom false)
               scheduled (atom false)]
           (fn []
             (if @running
               (reset! scheduled true)
               (do
                 (reset! running true)
                 (go-loop []
                   (let [now (js/Date.now)
                         delta-t (- now @prev-t)]
                     (reset! prev-t now)
                     (when (< delta-t t)
                       (<! (timeout (- t delta-t))))
                     (let [result (f)]
                       (when (chan? result)
                         (<! result)))
                     (if @scheduled
                       (do (reset! scheduled false)
                           (recur))
                       (reset! running false))))))))))
