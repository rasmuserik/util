(ns ^:figwheel-load solsort.apps.rasmuserik
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
    [solsort.util :refer [route log]]
    [reagent.core :as reagent :refer  []]
    [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]))


(def img-height 160)
(def small-size 12)
(def activity-height 130)
(defn html []
  [:div {:style {:textAlign :center}}
   [:div {:style {
                  :display :inline-block
                  :maxWidth 720
                  :textAlign :center
                  :fontSize 16 :marginTop (/ img-height 2) :marginBottom (/ img-height 2)}}
    [:img {:src "/icons/rasmuserik.jpg"
           :style {:height img-height
                   :width img-height
                   :borderRadius (/ img-height 10)
                   :verticalAlign :middle
                   :marginRight (/ img-height 8)
                   :marginLeft (/ img-height 8)
                   :boxShadow "0px 0px 2px #000"
                   }}]
    [:div {:style {:display :inline-block
                   :verticalAlign :middle
                   :textAlign :center
                   :margin 4
                   :marginRight (/ img-height 8)
                   :marginLeft (/ img-height 8)
                   }}

     [:h1 {:style {:marginBottom 10}} "Rasmus\u00a0Erik Voel\u00a0Jensen"]
     [:div {:style {:fontSize "100%"}}
      [:a {:href "/" :style {:fontSize "130%" :marginBottom 10}} "solsort.com"]
      "\u00a0ApS" [:br][:br]
      "Tingskrivervej\u00a021,\u00a03tv \u00a0 2400\u00a0København\u00a0NV \u00a0 \u00a0" [:br]
      "+45\u00a060703081 \u00a0 hej@solsort.com"
      ]]]
   [:div
    [:div {:style {:display :inline-block
                   :width 320
                   :verticalAlign :top
                   :textAlign :left
                   }}
     [:h1 "Professional"]
     [:h2 "Current"]
     [:ul {:style {:minHeight activity-height}}
      [:li "Write " [:a {:href "//github.com/rasmuserik"} "open source code"]
       ", focus on HTML5 Apps in ClojureScript"]
      [:li "Design and create solutions in collaboration with non-technical stakeholders"]
      [:li "Run " [:a {:href "//solsort.com"} "my own company"]
       ", with all the aspects of that"]]
     [:h2 "Experience"]
     [:div  {:style {:marginBottom 60}}
      "Work as developer and project manager • Conference talks •"
      " Taught/created university level courses • Master degree in Computer Science"]]
    [:div {:style {:display :inline-block
                   :width 320
                   :verticalAlign :top
                   :textAlign :left
                   }}
     [:h1 "Personal"]
     [:h2 "Current"]
     [:ul {:style {:minHeight activity-height}}
      [:li "Fatherhood - I am the father of a wonderful 3 year old boy"]
      [:li
       [:a {:href "http://www.swingshoes.dk/kalender-swingarrangementer/"} "Lindy Hop"] ", "
       [:a {:href "http://ci-cph.dk"} "Contact Impro"] ", "
       "Argentinsk\u00a0Tango" ", "
       "Kizomba" ", "
       "Salsa" ", "
       "Yoga"
       ]
      [:li
       [:a {:href "http://junto.dk"} "Junto"] ", "
       [:a {:href "http://tinkuy.dk"} "Tinkuy"] ", "
       "Rollespil(D&D3.5), Bicycle, Vegetarian"
       ]]
     [:h2 "Experience"]
     [:div {:style {:marginBottom 60}}
      "Toastmasters • photography • kbhff • mythology/religion/mysticism/energy • events • "
      "nature • massage • couchsurfing • music/jam • mensa • højskole"]]
    ]
   [:div {:style {:fontSize small-size}}
    "I very rarely use  social media (linkedin/facebook/twitter/...) so catch me "
    "in real life instead :)"
    [:br] "Updated Summer 2015"]]
  )

(route "rasmuserik"
       (fn []
         {:type :html
          :offline true
          :title "Rasmus Erik - solsort.com"
          :css {:h1 {:fontWeight :normal
                     :marginTop 0}
                :h2 {:fontSize small-size
                     :fontWeight :normal
                     :textAlign :left}}
          :html (html)}))
