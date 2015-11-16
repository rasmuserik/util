(ns solsort.apps.bib
  (:require-macros
    [reagent.ratom :as ratom]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [solsort.util :refer [log <ajax host route]]
    [solsort.misc :refer [<seq<!]]
    [solsort.db :refer [db-url]]
    [clojure.string :as string]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

; # BibApp

(def isbn-urls
  "urls of sample cover pages, used during development"
  (map
    #(str "http://bogpriser.dk/Covers/" (.slice % 7) "/978" % ".jpg")
    ["1451632828" "8702006568" "8759316283" "1408810637" "8776803483" 
     "8772817217" "8770700078" "1603200608" "8741257853" "8711417812"
     "0824724498" "1847876225" "8700474345" "0870707582" "0415878173"
     "1401309701" "1408313497" "1250028020" "1442485419" "8756785341"
     "0582819078" "8711319826" "8774671312" "8711380093" "1939521255"
     "8723509789" "8770624466" "8770823753" "8771390230" "8762711488"
     "8740011678" "8792246257" "0745331072" "0765316318" "8762715080"
     "8762644311" "8702168822" "0746096574" "1848355897" "0415228329"
     "8799040346" "8702062052" "8776641313" "8771089318" "8702075625"
     "1476816302" "8756789264" "8807018312" "8772814261" "8756782821"
     "0321442482" "8702029970" "8702085723" "8711319567" "0987090843"
     "1849494182" "0415494809" "8792397003" "8702080537" "0195381351"
     "0306814471" "8762605657" "1423134497" "0500544150" "1579908577"
     "0385608459" "8702146455" "1596436947" "8712043973" "0316732598"
     "0434019236" "8702066692" "0307382115" "0701185206" "1250025951"
     "0593064443" "8772815886" "8762717992" "8771056792" "0735623040"
     "1846861857" "8493668815" "8778389961" "0571249312" "8703054926"
     "0571273645" "1846682469" "8741102320" "8740011258" "1405353151"
     "8776690212" "0230250284" "8762716018" "8779735781" "8762654594"
     "8770660099" "1416573012" "1408331378" "1847245823" "8771050349"
     "8799339815" "8777026348" "1416984528" "1598800180" "8770626507"
     "1566917070" "8791947216" "8778875235" "8723030658" "1592537822"
     "0375857829" "0870707674" "0747810520" "0745660905" "0571220090"]))
(log isbn-urls)
(def w 70)
(def h 100)
(defn bibapp []
  [:div
   [:div
    [:input {:name "blah"
             :style {:height 40
                     :margin-left (* .5 w)
                     :margin (* .1 w)
                     :width (* w 7)
                     :font-size 30
                     }
             }] [:button "Søg"]
    
    ]
   (into 
    []
    (concat
      [:div {:style {:display :inline-block
                     :position :relative
                     :overflow "hidden"
                     :width (* w 8)
                     :height (* h 8)
                     :background "black"
                     }}]
      (map
        (fn [u x y] [:img {:src u 
                           :width w 
                           :height h 
                           :style {:position :absolute
                            ;       :filter "contrast(50%)"
                                   :outline "1px solid black"
                                   :webkit-filter 
                                   "contrast(40%) brightness(150%)"
                                   :left (* w (- x (* .5 (bit-and (identity y) 1))))
                                   :top (* h y)
                                           
                                   }
                           }])
        (take 100 isbn-urls)
        (cycle (range 10))
        (map #(js/Math.floor (/ % 10)) (range 100)))
      [[:div {:style {:position :absolute
                      :width "100%"
                      :height "100%"
                      :top 0
                      :left 0
                      :background "rgba(255,255,255,0.5)"
                      ;:box-shadow "3px 3px 15px #000 inset"
                      }}]]
      (map
        (fn [u x y] [:img 
                     {:src u 
                      :width (* 1.5 w) 
                      :height (* 1.5 h)
                      :style 
                      {:position :absolute
                       ;:border "1px solid black"
                       :outline "1px solid black"
                       :box-shadow "3px 3px 15px #000"
                       :left (* x w)
                       :top (* h (+ (/ 1 6) y) 1.5)
                               
                       }
                      }])
        (take 10 (drop 42 isbn-urls))
        (cycle [0.25 4.25 2.25 6.25])
        (map #(js/Math.floor (/ % 2)) (range 100)) 

        )

      ))]
  )
; #notes
; NB: http://ogp.me/, http://schema.org, dublin-core, https://en.wikipedia.org/wiki/RDFa



; - search
;   - søgehistorik
;   - søgeresultater
;   - materialevisning
; - patron
;   - lånerstatus
;   - åbn e-bog etc
; - libraries
;   - find biblioteker
;   - dette bibliotek
; - ask
;   - Spørg en bibliotekar


; mapping from:
; 
; - ting-id `_id`
; - cover (hasTingCover, isbn and not missing-bogpris-cover)
; - creator
; - abstract
; - subject
; - type
; - date
; - language
; - serieTitle
; - related
; - description
; - isbn
; - serieTitle
; - classification
;

; # layout experiments
(defn add-width-pos [boxes]
  (loop [box (first boxes)
         boxes (rest boxes)
         acc []
         w 0]
    ;    (log 'atw box w)
    (if (nil? box)
      acc
      (recur (first boxes)
             (rest boxes) 
             (conj acc
                   (assoc box
                          :width-pos
                          w))
             (+ w (:ratio box))))))
(defn width-partition [boxes n]
  (let [total-width (+ (:width-pos (last boxes))
                       (:ratio (last boxes)))]
    (group-by #(js/Math.floor (/ (* n (:width-pos %)) total-width))
              boxes)))

(defn width-height [boxes w]
  (let [boxes-width (reduce #(+ %1 (:ratio %2)) 0 boxes)
        boxes-height (/ w boxes-width)]
    (map 
      #(assoc %
              :height boxes-height
              :width (* (:ratio %) boxes-height))
      boxes)))

(defn box-layout []
  (def boxes 
    (->> (take 32 (range))
         (map
           (fn [i]
             (let [x (js/Math.random)
                   y (js/Math.random)
                   ]
               {:id i
                :coords [x y]
                :x x
                :y y
                :color (str "rgba(" (js/Math.floor (* 256 x)) "," (js/Math.floor (* 256 y)) ",128,1)")
                :ratio (+ 0.5 (js/Math.random))})
             ))
         (sort 
           (fn [a b]
             (- (:x a) (:x b))
             )) 
         (add-width-pos)
         ))


  ;(js/console.log (clj->js (width-partition boxes 8)))

  [:div
   {:style {:display "block"
            :position "relative"
            }}
   "blah"
   (into
     [:div]
     (map
       (fn [o]
         [:div "row"]
         )
       (map
         #(width-height % (- js/window.innerWidth 3))
         (width-partition boxes 8))
       )


     )
   (into 
     [:div
      {:style {:display "block"
               :position "relative"}}

      ]
     (map 
       (fn [o]
         [:div {:style {:position "absolute"
                        :display "inline-block"
                        :top (* 200 (:y o))
                        :left (* 500 (:x o))
                        :box-shadow "1px 1px 1px black"
                        :height 50
                        :width (* (:ratio o) 50)
                        :background-color (:color o)}}
          (str (:id o))])
       boxes))])

(route 
  "boxlayout"
  (fn [o]
    (go
      {:type :html
       :html [box-layout]
       }
      )))
; # default page
(defn <lidobj [lid] (<ajax (db-url (str "bib-old/" lid) )))
(defn <lid-info-obj [lid] (go (get (<! (<lidobj lid)) "info")))
(def sample-lids
  ["28511663" "28902239" "27999441" "27541062" "25862031"
   "20411724" "23917076" "29541167" "20476079" "29815860"
   "27594506" "25523911" "07203659" "44764873"])
(defn sample-lid [lid]
  (go
    [:li [:a {:href (str "/bibdata/lid/" lid)} lid]
     " " (first ((<! (<lid-info-obj lid)) "title"))]))
(defn <default []
  (go
    {:type :html
     :title " bibdata - solsort.com"
     :css {"body" {"margin" "5%"}
           ".spaceabove" {"margin-top" "1ex"}
           "ul" {"margin-top" "0"}}
     :html [:div.container
            [:h1 "BibData"]
            "Eksempler:"
            (into [:ul] (<! (<seq<! (map sample-lid sample-lids))))
            [:small "Eksemplerne er udvalgt som 1., 10., 100., 1.000., 10.000., 20.000., 30.000., 40.000., 50.000., 60.000., 70.000., 80.000., 90.000., og 100.000. mest populære bog."]
            ]}))


; # show-material

(def biblioteker [["bibliotek.dk" "http://bibliotek.dk/linkme.php?rec.id=870970-basis:"] ])

(defn typename [o]
  (case (first (o "type"))
    "book" "Bog"
    "Bog" "Bog"
    "Billedbog" "Bog"
    "Dvd" "Film"
    "Tidskriftasaf" "Artikel"
    (first (o "type"))))

(defn itemtype [o]
  (str "http://schema.org/"
       (case (typename o)
         "Bog" "Book"
         "Film" "Movie"
         "Artikel" "Article"
         (do
           (log 'bibdata 'warning-missing-itemtype (o "type"))
           "CreativeWork"))))


(defn html-for-type [k vs o]
  (case k
    "title" [:h1 {:itemProp "name"} (first vs)]
    "abstract" [:p {:itemProp "description"} (first vs)]
    "creator" (into [:h2 "af "]
                    (interpose
                      " & "
                      (map (fn [v] [:span {:itemProp "creator"} v])
                           vs)))
    "date" [:div (typename o) " udgivet " [:span {:itemProp "datePublished"} (first vs)]]
    "classification" [:div "DK5: " (string/join " & " vs)]
    "type" [:div "type: " (first vs)]
    "isbn" [:div "ISBN: " [:span {:itemProp "isbn"} (first vs)]]
    "related" 
    [:p
     [:strong "Relaterede biblioteksmaterialer:"] (into [:ul]
                                                        (map (fn [[id _1 _2 title]]
                                                               [:li [:a {:href (str "/bibdata/ting/" id)} title]]

                                                               ) vs)
                                                        )]
    [:div k (str vs)]))


(defn <bibobj [id] (<ajax (db-url (str "bib/" id) )) )
(defn show-recommendations [o]
  )
(defn show-obj [o]
  (let [isbn (first (o "isbn"))
        title (first (o "title"))
        cover-src (if (and isbn (o "bogprisCover"))
                    (str "//www.bogpriser.dk/Covers/" (.slice isbn -3) "/" isbn ".jpg")
                    )
        ks (filter o ["title" "creator" "date" "abstract"
                      ;"classification" ;"serieTitle"
                      "isbn" "related"])
        ;         ks (keys o)

        ]
    (into 
      [:div.scol {:itemScope "itemscope"
                  :itemType (itemtype o)}
       [:div.wl6.wm8.ws9.scol.right [:img {:width "100%"
                                           :src cover-src}]]]

      (map #(html-for-type % (o %) o) ks)
      ))
  )
(defn show-item [info o]
  (js/console.log "bib show-item" (clj->js o))

  (show-obj o))

#_(go
    (let [meta (<! (<bibobj "meta"))]
      (js/console.log (clj->js meta))
      ))

(defn bibitem [info o]
  {:type :html
   :html [:div.scontain
          [show-item info o] 
          [:hr]
          [:p
           [:small 
            "Biblioteks-app-prototype (in progress). Data stammer fra DBC: 
            enten åbne data, eller via webservice på dev.vejlebib.dk. 
            Forsideillustrationer vises via links links til dev.vejlebib.dk, 
            eller bogpriser.dk, og hostes ikke direkte fra dette site."]]] })

; # hello
(defn <hello [o]
  (go 
    (let [meta (<! (<bibobj "meta"))
          ids (shuffle (meta "annotated")) ]
      (loop [id (first ids)
             ids (rest ids)]
        (if id
          (let
            [o (<! (<bibobj id))
             hasCover (first (o "hasTingCover"))
             title (first (o "title"))
             creator (string/join " & " (get o "creator" []))
             desc (first (o "abstract"))
             tags (string/join " " (get o "classification" []))
             id (o "_id")
             occurrences (second (first (o "related")))
             obj (clj->js {:title title :creator creator :desc desc :tags tags}) ]
            (when hasCover
              (<! (<ajax (str "http://localhost:9200/bib/ting/" id) :method "PUT" :data obj)))

            (recur (first ids) (rest ids)))
          nil
          ))
      (js/console.log (clj->js meta))
      {:type "text/plain"
       :content "hello"
       }

      )))
; # route bibdata
(defn <lid [lid] (<ajax  (db-url  (str "bib-old/" lid) ) :result :json))
(defn <lidjson [part lid]
  (go {:type :json :json (aget (or (<! (<lid lid)) #js {}) part) }))


(defn route-fn [info]
  (let [path (string/split (info "path") "/")
        id (nth path 2 "")
        kind (nth path 1 "")]
    (case kind
      "related" (<lidjson "related" id)
      "info" (<lidjson "stat" id)
      "lid" (go 
              (bibitem
                info
                (-> id (<lid) (<!)
                    (aget "info") (aget "id") (aget 0)
                    (<bibobj) (<!))))
      "ting" (go (bibitem info (<! (<bibobj id))))
      "bibapp" {:type :html :html  (bibapp)}
      (<default))))

(route "bib" route-fn)
(route "bibdata" route-fn)
