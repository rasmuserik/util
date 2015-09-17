(ns solsort.style
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [clojure.string :as string :refer  [split]]
    [clojure.string :refer  [join]]
    ))

(def is-figwheel (some? js/window.figwheel))
(when is-figwheel (js/setTimeout #(run-tests nil) 0))

;; # css
(defn css-name [id]
  (clojure.string/replace (name id) #"[A-Z]" #(str "-" (.toLowerCase %1))))
#_(testcase 'css-name
            #(= (css-name :FooBar) "-foo-bar"))
(defn handle-rule [[k v]]
  (str (css-name k) ":" (if (number? v) (str v "px") (name v))))
(defn handle-block [[id rules]]
  (str (name id) "{" (join ";" (map handle-rule (seq rules))) "}"))
(defn clj->css [o]
  (join (map str (seq o))) (join (map handle-block (seq o))))
(defn js->css [o] (clj->css (js->clj o)))

(testing "css"
  (is (= (clj->css {:h1 {:fontWeight :normal :fontSize 14} :.div {:background :blue}})
         "h1{font-weight:normal;font-size:14px}.div{background:blue}")))
(is (= (clj->css [[:h1 {:fontWeight :normal :fontSize 14}]
                  [:.div {:background :blue}]
                  ["h1" {:background :red}]
                  ])
       "h1{font-weight:normal;font-size:14px}.div{background:blue}h1{background:red}"))

(def default-style
  (atom { "@font-face" {:fontFamily "Ubuntu"
                        :fontWeight "400"
                        :src "url(/font/ubuntu-latin1.ttf)format(truetype)"}
         :.inline-block {:display "inline-block"}
         :.container {:margin "5%" }
         :.button {:margin 5 :padding 5 :borderRadius 5 :border "1px solid black"}
         :body {:margin 0 :padding 0 :fontFamily "Ubuntu, sans-serif"}
         :.hidden {:display "none"}
         :div {:margin 0 :padding 0} }))

(defn load-default-style! []
  (aset (or (js/document.getElementById "default-style")
                 (let [elem (js/document.createElement "style")]
                   (aset elem "id" "default-style")
                   (.appendChild js/document.head elem) 
                   elem))
        "innerHTML" (clj->css @default-style)))
(load-default-style!)
