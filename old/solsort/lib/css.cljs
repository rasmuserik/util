(ns solsort.lib.css
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
    [solsort.sys.test :refer [testcase]]
    [solsort.sys.mbox :refer [route log]]
    [clojure.string :refer [join]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]))


(defn css-name [id]
  (clojure.string/replace (name id) #"[A-Z]" #(str "-" (.toLowerCase %1))))
(testcase 'css-name
          #(= (css-name :FooBar) "-foo-bar"))
(defn handle-rule [[k v]]
  (str (css-name k) ":" (if (number? v) (str v "px") (name v))))
(defn handle-block [[id rules]]
  (str (name id) "{" (join ";" (map handle-rule (seq rules))) "}"))
(defn clj->css [o]
  (join (map str (seq o))) (join (map handle-block (seq o))))
(defn js->css [o] (clj->css (js->clj o)))

(testcase 'clj->css
          #(= (clj->css {:h1 {:fontWeight :normal :fontSize 14} :.div {:background :blue}})
              "h1{font-weight:normal;font-size:14px}.div{background:blue}"))

(def default-style
  (atom { "@font-face" {:fontFamily "Ubuntu"
                        :fontWeight "400"
                        :src "url(/font/ubuntu-latin1.ttf)format(truetype)"}
         :.container {:margin "5%" }
         :.button {:margin 5 :padding 5 :borderRadius 5 :border "1px solid black"}
         :body {:margin 0 :padding 0 :fontFamily "Ubuntu, sans-serif"}
         :div {:margin 0 :padding 0} }))

(route "style"
       #(go (clj->js {:http-headers {:Content-Type "text/css"}
                      :content (clj->css @default-style)})))
