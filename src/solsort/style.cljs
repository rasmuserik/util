(ns solsort.style
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [clojure.string :as string :refer  [split]]
    [solsort.misc :refer [log]]
    [clojure.string :refer  [join]]
    ))

(def is-figwheel (some? js/window.figwheel))
(when is-figwheel (js/setTimeout #(run-tests nil) 0))

;; # normalize-cs s
(def normalize-css
  (str "/*! normalize.css v3.0.3 | MIT License | github.com/necolas/normalize.css"
       " */html{font-family:sans-serif;-ms-text-size-adjust:100%;-webkit-text-size"
       "-adjust:100%}body{margin:0}article,aside,details,figcaption,figure,footer,"
       "header,hgroup,main,menu,nav,section,summary{display:block}audio,canvas,"
       "progress,video{display:inline-block;vertical-align:baseline}audio:not(["
       "controls]){display:none;height:0}[hidden],template{display:none}a{"
       "background-color:transparent}a:active,a:hover{outline:0}abbr[title]{border"
       "-bottom:1px dotted}b,strong{font-weight:bold}dfn{font-style:italic}h1{font"
       "-size:2em;margin:.67em 0}mark{background:#ff0;color:#000}small{font-size:"
       "80%}sub,sup{font-size:75%;line-height:0;position:relative;vertical-align:"
       "baseline}sup{top:-0.5em}sub{bottom:-0.25em}img{border:0}svg:not(:root){"
       "overflow:hidden}figure{margin:1em 40px}hr{box-sizing:content-box;height:0}"
       "pre{overflow:auto}code,kbd,pre,samp{font-family:monospace,monospace;font-"
       "size:1em}button,input,optgroup,select,textarea{color:inherit;font:inherit;"
       "margin:0}button{overflow:visible}button,select{text-transform:none}button,"
       "html input[type=\"button\"],input[type=\"reset\"],input[type=\"submit\"]{-"
       "webkit-appearance:button;cursor:pointer}button[disabled],html input["
       "disabled]{cursor:default}button::-moz-focus-inner,input::-moz-focus-inner{"
       "border:0;padding:0}input{line-height:normal}input[type=\"checkbox\"],input"
       "[type=\"radio\"]{box-sizing:border-box;padding:0}input[type=\"number\"]::-"
       "webkit-inner-spin-button,input[type=\"number\"]::-webkit-outer-spin-button"
       "{height:auto}input[type=\"search\"]{-webkit-appearance:textfield;box-"
       "sizing:content-box}input[type=\"search\"]::-webkit-search-cancel-button,"
       "input[type=\"search\"]::-webkit-search-decoration{-webkit-appearance:none}"
       "fieldset{border:1px solid silver;margin:0 2px;padding:.35em .625em .75em}"
       "legend{border:0;padding:0}textarea{overflow:auto}optgroup{font-weight:bold"
       "}table{border-collapse:collapse;border-spacing:0}td,th{padding:0}"
       ))

;; # css
(declare clj->css)
(defn css-name [id]
  (clojure.string/replace (name id) #"[A-Z]" #(str "-" (.toLowerCase %1))))
#_(testcase 'css-name
            #(= (css-name :FooBar) "-foo-bar"))
(defn handle-rule [[k v]]
  (cond 
    (number? v) (str (css-name k) ":" v "px;") 
    (map? v) (str (name k) "{" (join ""  (map handle-rule  (seq v))) "}")
    :else (str (css-name k) ":" (name v) ";")))
(defn handle-block [[id rules]]
  (str (name id) "{" (join "" (map handle-rule (seq rules))) "}"))
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
  (atom [["@font-face" {:fontFamily "Ubuntu"
                        :fontWeight "400"
                        :src "url(//solsort.com/font/ubuntu-latin1.ttf)format(truetype)"}]
         [:.inline-block {:display "inline-block"}]
         [:.container {:margin "5%" }]
         [:.ccc {:background "#ccc"}]
         [:.button {:margin 5 :padding 5 :borderRadius 5 :border "1px solid black"}]
         [:.hidden {:display "none"}]
         [:.noclear {:clear "none"}]
         [:.right {:float "right"}]
         [:.left {:float "left"}]
         [:.scontain {:width "100%"
                     :max-width 960
                     :margin "auto"
                     }]
         [:.scol {:padding-left 8 
                  :vertical-align "top"
                  :padding-right 8
                  :box-sizing "border-box"}]
         ["@media (min-width: 600px)" 
          {:.scol {:padding-left 12
                  :padding-right 12
                  :box-sizing "border-box"}}]
         [:div {:margin 0 :padding 0}] ]))

(swap! default-style
       into (map (fn [[id w]]
[(str "@media " w)
 (into {}
 (for [i (range 1 25)]
  [(str id i)
    {:width (str (/ (js/Math.floor (/ i .0024)) 100) "%")
    :display "inline-block" }]))])
              [[".ws" "all"]
               [".wm" "(min-width:480px)"]
               [".wl" "(min-width:840px)"]]  
     
     ))
#_(loop [i 1]
  (swap! default-style
         into
         (map (fn [[id w]]
                [(str "@media " w)
                 {(str id i)
                  {:width (str (/ (js/Math.floor (/ i .0024)) 100) "%")
                   :display "inline-block" }}])
              [[".ws" "all"]
               [".wm" "(min-width:480px)"]
               [".wl" "(min-width:840px)"]]))
  (when (<= i 24) (recur (inc i))))
(log @default-style (clj->css @default-style))

(defn add-default-style [o] (swap! default-style into o))
(defn default-style-str [] 
  (str normalize-css 
       "\n/*! solsort-util css | github.com/rasmuserik/solsort-util */" 
       (clj->css @default-style)))
(defn load-default-style! []
  (aset (or (js/document.getElementById "default-style")
            (let [elem (js/document.createElement "style")]
              (aset elem "id" "default-style")
              (.appendChild js/document.head elem) 
              elem))
        "innerHTML" 
        (default-style-str)))

(defn style [o] [:style {"dangerouslySetInnerHTML" #js {:__html (clj->css o)}}])
;; # kitchen sinkg
(def 
  kitchensink
  [:div.scontain "Responsive grid:"
   [:div.srow
    [:div.ws1.scol [:p.ccc "."]]
    [:div.ws2.scol [:p.ccc "12"]]
    [:div.ws6.scol [:p.ccc "1/4"]]
    [:div.ws15.scol [:p.ccc "golden"]]]
   [:div.srow
    [:div.ws2.scol [:p.ccc "12"]]
    [:div.ws2.scol [:p.ccc "12"]]
    [:div.ws2.scol [:p.ccc "12"]]
    [:div.ws18.scol [:p.ccc "3/4"]]]
   [:div.srow
    [:div.ws12.scol [:p.ccc "1/2"]]
    [:div.ws8.scol [:p.ccc "1/3 "]]
    [:div.ws4.scol [:p.ccc "1/6 "]]] 
   [:div.srow
    [:div.wl4.wm6.ws8.scol [:p.ccc "re"]]
    [:div.ws8.wm6.wl4.scol [:p.ccc "sp"]]
    [:div.ws8.wm6.wl4.scol [:p.ccc "on"]]
    [:div.ws8.wm6.wl4.scol [:p.ccc "si"]]
    [:div.ws8.wm6.wl4.scol [:p.ccc "ve"]]
    [:div.ws8.wm6.wl4.scol [:p.ccc ".."]]]
   ]
  )
