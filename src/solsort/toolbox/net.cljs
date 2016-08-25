(ns solsort.toolbox.net
  (:require-macros
    [cljs.core.async.macros :refer  [go alt!]])
  (:require
    [goog.net.XhrIo]
    [clojure.string :as string]
    [solsort.toolbox.misc :refer [<p async-err]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

(defn <load-js
  "Load a JavaScript file, and emit true on returned channel when done"
  [url]
  (let [c (chan)
        elem (js/document.createElement "script")]
    (js/document.head.appendChild elem)
    (aset elem "onload" (fn [] (put! c true)))
    (aset elem "src" url)
    c))

;; # crypto
(defn utf16->utf8 [s] (js/unescape (js/encodeURIComponent s)))
(defn utf8->utf16 [s] (js/decodeURIComponent(js/escape s)))
(defn buf->utf8-str [a] (string/join (map #(js/String.fromCharCode %) (seq (js/Array.prototype.slice.call (js/Uint8Array. a))))))
(defn buf->str [a] (utf8->utf16 (buf->utf8-str a)))
(defn utf8-str->buf [s] (js/Uint8Array.from (clj->js (map #(.charCodeAt % 0) s))))
(defn str->buf [s] (utf8-str->buf (utf16->utf8 s)))
; TODO: no-atom here
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

(defn <ajax ; # <ajax
  [url & {:keys [method data headers timeout credentials result]
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
            (put! c (async-err e)))))
      method data (clj->js headers) timeout credentials)
    c))
