(ns solsort.lib.html
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
    [cljsjs.react]
    [solsort.lib.css :refer [js->css]]
    [solsort.sys.platform :refer [is-browser]]
    [solsort.sys.test :refer [testcase]]
    [clojure.string :refer [split join]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]))


; html components and events
(def renders (atom {}))
(def handlers (atom {}))
(defn render [path f] (swap! renders assoc path f))
(defn events [path f] (swap! renders assoc path f))
; (render "foo" (fn [state obj] [:div "foo"]))
; NB: if event happens during render, cancel render
;
; (events "foo" (fn [state kvs] [state kvs]))
; NB: rerender after event done
;


; handle user-mutable state
(def root (atom nil))
(def values (atom {}))
(declare render-html)
(defn inputchange [event]
  (let [target (aget event "target")
        name (aget target "name")
        value (aget target "value")]
    (swap! values assoc name value)
    (render-html @root)
    ))
(defn input-handler[state [input o & children]]
  (let [name (:name o)
        value (@values name)
        o (if value (assoc o "value" value) o)]
    (into [input (assoc o "onChange" inputchange)] children)
    ))


(when is-browser
  (render "input" input-handler)
  (render "textarea" input-handler)
  (render "select" input-handler))

; transformation of html-sexpr to react
(defn parse-classes [head prop]
  (let [
        tagname (re-find #"^[^.#]*" head)
        [_ id] (re-find #"[#]([^.#]*)" head)
        prop (if id (assoc prop "id" id) prop)
        prop
        (if (re-find #"[.]" head)
          (let
            [classes #{}
             classes (into classes (split (or (prop "className") "") " "))
             classes (into classes (map second (re-seq #"[.]([^.#]*)" head)))
             classes (join " " classes)]
            (assoc prop "className" classes))
          prop)
        ]
    [tagname prop]
    ))

(testcase 'parse-class-none
          #(= (parse-classes "foo" {}) ["foo" {}]))
(testcase 'parse-class
          #(= (parse-classes "foo.bar#baz.Quux" {"className" "hi lo"}) ["foo" {"className" "hi lo bar Quux" "id" "baz"}]))

(defn clj->react [node]
  (if-not (sequential? node)
    node
    (let [
          has-properties (map? (second node))
          tail (if has-properties
                 (drop 2 node)
                 (drop 1 node))
          tail (map clj->react tail)
          prop (if has-properties (second node) {})
          head (name (first node))
          [head prop] (parse-classes head prop)
          render (@renders head)
          [head prop & tail] ((or render (fn [a b] b)) {} (into [head prop] tail))
          ]
      (apply js/React.createElement head (clj->js prop) tail)
      )))

(testcase 'clj->react-1
          #(= (js/React.renderToStaticMarkup
                (clj->react
                  [:div.foo [:span#foo "hello"]]))
              "<div class=\"foo\"><span id=\"foo\">hello</span></div>"
              ))


; html-document to http/browser
(defn html->http [o]
  ;(clj->react  (:html o))
  #js{:http-headers #js{"Content-Type" "text/html;charset=UTF-8"}
      :content
      (str
        "<!DOCTYPE html><html"
        (if (:offline o) " manifest=\"/solsort.appcache\"" "")
        "><head>"
        "<title>" (or (:title o) "solsort.com") "</title>"
        "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\">"
        "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">"
        "<meta name=\"viewport\" content=\"" "width=device-width, initial-scale=1.0"
        (if (:noscale o) ", minimum-scale=1.0, maximum-scale=1.0, user-scalable=0" "") "\">"
        "<meta name=\"format-detection\" content=\"telephone=no\">"
        "<link href=/style.css rel=stylesheet>"
        "<style id=style>" (if (:css o) (js->css (clj->js (:css o)))) "</style>"
        "</head><body>"
        (or (:rawhtml o)
            (js/React.renderToStaticMarkup (clj->react (:html o)))
            )
        "<script src=\"/solsort.js\"></script>"
        "</body></html>")})
(defn render-html [o]
  (reset! root o)
  (if (:css o)
    (let [style-elem
          (or (js/document.getElementById "style")
              (let [elem (js/document.createElement "style")]
                (aset elem "id" "style")
                (js/document.head.appendChild elem)
                elem))
          css-str (js->css (clj->js (:css o)))
          ]
      ; TODO: actually change style rules in document.styleSheets
      (aset style-elem "innerHTML" css-str)))
  (if (:rawhtml o)
    (aset js/document.body "innerHTML" (:rawhtml o))
    (js/React.render (clj->react (:html o)) js/document.body))
  (if (:title o)
    (aset (aget (js/document.getElementsByTagName "title") 0) "innerHTML" (:title o)))
  true)
