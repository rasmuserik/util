(ns solsort.apps.index
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
    [reagent.core :as reagent :refer  []]
    [solsort.core :refer [route log canonize-string hex-color]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]))


(def entries (atom []))
(defn add-entry [title tags url]
  (swap! entries conj {:title title :tags tags :url url}))


(def circle-size 100)
(defn entry [o]
  (let [title (:title o)]
    [:a { :href (:url o)
         :style {:width circle-size
                 :height circle-size
                 :margin 8
                 :display :inline-block
                 :textAlign :left
                 :borderRadius (/ circle-size 2)
                 :boxShadow (str "0px 0px 2px #000, "
                                 "3px 3px 10px rgba(0,0,0,0.4)")
                 }}
     [:img {:src (str "/icons/" (canonize-string title) "")
            :style { :width circle-size
                    :height circle-size
                    :backgroundColor "#fff"
                    :position :absolute
                    :margin 0
                    :padding 0
                    :borderRadius (/ circle-size 2)
                    }}]
     [:div
      {:style {
               :display "inline-block"
               :height circle-size
               :width circle-size
               :position :absolute
               :lineHeight (str circle-size "px")
               :textAlign :center
               :fontWeight :bold
               :color :black; (hex-color (bit-and 0x3f3f3f (hash title)))
               :backgroundColor "rgba(255,255,255,0.3)"
               :borderRadius (/ circle-size 2)
               :textShadow (str
                             "2px 2px 10px #fff,"
                             "2px -2px 10px #fff,"
                             "-2px 2px 10px #fff,"
                             " -2px -2px 10px #fff")
               :fontSize (bit-or 0 (* circle-size 0.16))
               }}
      [:span
       {:style {:display "inline-block"
                :verticalAlign "middle"
                :width circle-size
                :lineHeight :normal
                :zIndex 10
                }}
       title]
      ]]))

(defn home-html []
  [:div {:style {:textAlign :center}}
   [:div {:style {:margin "32px 0 64px 0" :fontSize 16}}
    [:img {:src "/icons/solsort.png"
           :style {:height 64 :width 64}}]
    [:div
     [:span {:style {:fontSize "150%"}}
      " solsort.com "]
     "ApS"]
    [:div
     "Open Source • Agile • Full Stack • ClojureScript"]
    [:div {:style {:fontSize "300%" :margin "0.5ex 0 1ex 0"}}
     "HTML5 Apps &\u00a0Backend"]
    [:div
     "kontakt: Rasmus Erik Voel Jensen" [:br]
     "+45 60703081 hej@solsort.com"
     ]
    ]
   [:div {:style {:textAlign :center}} (into [:div {}] (map entry @entries))]
   ])


(route :default
       (fn []
         (reagent/render-component  [home-html] js/document.body)
         {:offline true :type "html" :title "solsort.com" :html (home-html)}))

; state: unfinished|alpha|beta|done
(add-entry "Rasmus Erik Voel Jensen"
           ["developer" "company owner" "computer scientist"]
           "/rasmuserik.html")

(add-entry "Blog"
           ["2015"]
           "/blog/")
(add-entry "BibData"
           ["2015"]
           "/bibdata/")

(add-entry "Barefoot Tango"
           ["2015"]
           "/notes/barefoot-tango")
(add-entry "Repeat record"
           ["2015" "utility" "webapp" "firefox-only" "video"]
           "/#repeat-record/10")
(add-entry "Anbefalings-webservice"
           ["2015" "beta" "visualisering af relationer" "webservice" "ClojureScript"]
           "/visualisering-af-relationer/compare.html#relvis/cir870970-basis:05625351")
(add-entry "Visualisering af relationer"
           ["2014" "done" "visualisering af relationer" "visualisation" "JavaScript"]
           (str "https://vejlebib.dk/search/ting/musik#relvis/str870971-tsart:71029824,"
                "870971-tsart:71829375,870970-basis:49295642,870970-basis:07872992,"
                "870971-tsart:34418616,870970-basis:23454963,870970-basis:00117250,"
                "870971-tsart:73914493,870971-tsart:70501198,870971-tsart:70357151,"
                "870971-tsart:73443911,870970-basis:05385210,870970-basis:25722027,"
                "870970-basis:20269545,870970-basis:28902700,870970-basis:28799918,"
                "870971-tsart:33801262,870971-tsart:73950031,870970-basis:23292637,"
                "870970-basis:20826592,870970-basis:04971914,870970-basis:28799950,"
                "870970-basis:28799942,870970-basis:28205899,870970-basis:26386896,"
                "870970-basis:23702630,870970-basis:51445481,870970-basis:26747953,"
                "870971-tsart:87018148,870971-tsart:35714006i"))
(add-entry "Sketch note draw"
           ["2014" "beta" "webapp" "infinite canvas" "zoomable"]
           "/sketch-note-draw/")
(add-entry "Frie Børnesange"
           ["2014" "alpha" "webapp" "open content" "sangbog"]
           "/frie-sange/")
(add-entry "Learn morse\u00a0code"
           ["2014" "alpha" "webapp"]
           "/morse-code/")
(add-entry "Single touch snake"
           ["2014" "unfinished" "game" "webapp"]
           "/single-touch-snake/")
(add-entry "Parkering i København"
           ["2014" "alpha" "hackathon" "open data day" "data.kk.dk" "gatesense"
            "iotpeople" "okfn"]
           "/kbh-parking/")
(add-entry "360º Viewer"
           ["2014" "done" "widget" "frontend" "hammertime"]
           "/360/test.html")
(add-entry "Backend for UCC-organismen"
           ["2014" "done" "backend" "UCC Organismen" "ucc" "webuntis" "rejseplanen"]
           "http://ssl.solsort.com:8080/")
(add-entry "BibTekKonf Slides"
           ["2013" "done" "presentation" "dbc" "bibgraph"]
           "/slides/bibtekkonf2013-bibgraph")
(add-entry "Art quiz"
           ["2013" "alpha" "prototype" "hack4dk"]
           "/hack4dk/quiz/")
(add-entry "Summer\u00a0Hacks Slides"
           ["2013" "done" "copenhagenjs" "presentation" "bibgraph" "skolevej"]
           "/slides/cphjs2013-summer-hacks")
(add-entry "BibGraph"
           ["2013" "alpha" "visualisation" "widget" "dbc" "adhl" "d3"]
           "http://labs.dbc.dk/bibgraph")
(add-entry "HTML5 Developer Perspective Slides"
           ["2013" "done" "presentation" "html5" "cnug"]
           "/slides/talk-html5-2013/cnug2013-slides.html")
(add-entry "Speeding visualisation"
           ["2013" "done" "visualisation" "hammertime"
            "role:optimisation" "role:reimplementation"]
           "http://speeding.solsort.com/")
(add-entry "Dragimation"
           ["2013" "done" "widget" "hammertime" "legoland billund resort"]
           "http://dragimation.solsort.com")
(add-entry "Pricing scale"
           ["2013" "done" "notes" "estimation tool"]
           "/notes/pricing-scale")
(add-entry "Tsar Tnoc"
           ["2012" "beta" "ludum dare" "hackathon"]
           "/tsartnoc/")
(add-entry "EuroCards"
           ["2012" "done" "card game"]
           "https://www.thegamecrafter.com/games/EuroCards")
(add-entry "BlobShot"
           ["2012" "alpha" "game" "hackathon" "5apps hackathon"]
           "/blobshot/")
(add-entry "CombiGame"
           ["2012" "alpha" "game" "hackathon"]
           "http://old.solsort.com/#combigame")
(add-entry "Presentation evaluation notes"
           ["2012" "done" "notes" "toastmasters"]
           "/notes/presentation-evaluation")
#_(add-entry "NoteScore"
           ["2011" "beta" "app" "music" "edu"]
           "https://play.google.com/store/apps/details?id=dk.solsort.notescore")
(add-entry "Danske Byer"
           ["2011" "alpha" "edu"]
           "http://solsort.com/danske-byer")
(add-entry "CuteEngine"
           ["2011" "unfinished" "game" "unfinished"]
           "http://solsort.com/cute-engine")


;(def cached-file (memoize read-file-sync))
(def cached-file "not loaded")

(route "icons"
       (fn []
        #js{:http-headers #js{:Content-Type "text/plain"}
            :content (cached-file "misc/white.png")}))
