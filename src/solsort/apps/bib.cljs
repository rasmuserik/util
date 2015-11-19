(ns solsort.apps.bib ; #
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is]]
    [goog.net.XhrIo]
    [goog.net.Jsonp]
    [goog.object]
    [solsort.util :refer [log <ajax host route]]
    [solsort.ui :refer [input]]
    [solsort.misc :refer [<seq<! unique-id]]
    [re-frame.core :as re-frame :refer  [register-sub subscribe register-handler dispatch dispatch-sync]]
    [solsort.db :refer [db-url]]
    [clojure.string :as string]
    [reagent.core :as reagent :refer []]
    [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]))

; # BibApp
; TODO: extract common styling to classes
; ## configuration
(def background-color "black")
(def header-space 2)
(def view-width 16)
(def view-height (+ header-space 18.5))
(def widget-height (- view-height header-space 2.5))

(def isbn-urls ; ##
  "urls of sample cover pages, used during development"
  (cycle
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
       "0375857829" "0870707674" "0747810520" "0745660905" "0571220090"])))
(def ting-objs  ; ##
  (cycle
      (shuffle 
        ["870970-basis:28995946" "870970-basis:28995814" "830060-katalog:24120236"
       "870970-basis:28530439" "870970-basis:29094055" "870970-basis:22639862"
       "870970-basis:51567064" "870970-basis:29687579" "870970-basis:29404313"
       "870970-basis:20757299" "870970-basis:22965492" "870970-basis:26240549"
       "870970-basis:29525102" "870970-basis:27463061" "870970-basis:28308108"
       "870970-basis:27398898" "870970-basis:26499518" "870970-basis:26940702"
       "870970-basis:27193323" "870970-basis:51301242" "870970-basis:21840769"
       "870970-basis:51571959" "870970-basis:50979075" "870970-basis:29524831"
       "870970-basis:45305112" "870970-basis:29973512" "870970-basis:51468686"
       "870970-basis:29741034" "870970-basis:50959082" "870970-basis:26628075"
       "870970-basis:51689445" "870970-basis:24741133" "870970-basis:22893645"
       "870970-basis:50712389" "870970-basis:25724100" "870970-basis:26247888"
       "870970-basis:51128664" "870970-basis:29646066" "870970-basis:28185871"
       "870970-basis:26768322" "870970-basis:29567956" "870970-basis:29567875"
       "870970-basis:29879893" "870970-basis:29567980" "870970-basis:27931928"
       "870970-basis:29040427" "870970-basis:29727198" "870970-basis:25546199"
       "870970-basis:51413849" "870970-basis:29869014" "870970-basis:27891535"
       "870970-basis:28644248" "870970-basis:28849532" "870970-basis:28437064"
       "870970-basis:28943636" "870970-basis:21821470" "870970-basis:20133643"
       "870970-basis:23500663" "870970-basis:24690423" "870970-basis:23527545"
       "870970-basis:28421753" "870970-basis:50826880" "870970-basis:24587770"
       "870970-basis:24653161" "870970-basis:27276806" "870970-basis:24945669"
       "870970-basis:28995938" "870970-basis:28995849" "870970-basis:28995822"
       "870970-basis:28002947" "870970-basis:29239134" "870970-basis:29239142"
       "870970-basis:50989682" "870970-basis:51076699" "870970-basis:50557499"
       "870970-basis:27928420" "870970-basis:28417888" "870970-basis:28273177"
       "870970-basis:28427654" "870970-basis:28474709" "870970-basis:22808370"
       "870970-basis:22435051" "870970-basis:25915461" "870970-basis:23066475"
       "870970-basis:24372480" "870970-basis:22208470" "870970-basis:28552408"
       "870970-basis:29238596" "870970-basis:28138504" "870970-basis:22958496"])))

; ## subscriptions: :books :back-positions :front-positions :saved-positions :step-size :query
(register-sub :books (fn [db] (reaction (get @db :books []))))
(register-handler :reset-books (fn [db [_ books]] (assoc db :books books)))

(register-sub :back-positions (fn [db] (reaction (get @db :back-positions []))))
(register-handler :back-positions 
                  (fn [db [_ back-positions]] (assoc db :back-positions back-positions)))

(register-sub :front-positions (fn [db] (reaction (get @db :front-positions []))))
(register-handler :front-positions 
                  (fn [db [_ front-positions]] (assoc db :front-positions front-positions)))

(register-sub :query (fn [db] (reaction (get @db :query))))
(register-handler :query (fn [db [_ q]] (assoc db :query q)))

(register-sub :step-size (fn [db] (reaction (get @db :step-size))))
(register-handler :step-size (fn [db [_ step-size]] (assoc db :step-size step-size)))

(register-sub 
  :ting 
  (fn [db [_ id]] 
    (reaction (get-in @db [:ting id] {}))))
(register-handler 
  :ting (fn [db [_ id o]] 
          (assoc-in db [:ting id] (into (get-in db [:ting id] {}) o))))

; ## :*-positions :books initialisation
(defn epsilon [] (* 0.00001 (- (js/Math.random) (js/Math.random))))

(defn set-id [type os] 
  (map #(into %1 {:id [type %2] :x (+ (:x %1) (epsilon)) :y (+ (:y %1) (epsilon))}) 
       os (range)))

(dispatch-sync 
  [:back-positions
   (set-id :back
            (map (fn [x y] {:x x :y (+ y header-space) :size 2 :pos :back})
                 (cycle (concat (range 1 17 2) (range 0 17 2)))
                 (concat (repeat 8 1) (repeat 9 3)
                         (repeat 8 5) (repeat 9 7)
                         (repeat 8 9) (repeat 9 11)
                         (repeat 8 13) (repeat 9 15))))])

(dispatch-sync
  [:front-positions
   (set-id :front
            (concat
              (map #(into % {:y (+ header-space (:y %)) :size 3 :pos :front})
                   [{:x 2 :y 2} {:x 10 :y 2}
                    {:x 6 :y 5} {:x 14 :y 5}
                    {:x 2 :y 8} {:x 10 :y 8}
                    {:x 6 :y 11} {:x 14 :y 11}
                    {:x 2 :y 14} {:x 10 :y 14}])
              (map (fn [x] {:x x :y (- view-height 1) :size 1.8 :pos :saved}) 
                   (range 1 17 2))))])

(dispatch-sync
  [:reset-books
   (into {} (->> (concat @(subscribe [:front-positions]) @(subscribe [:back-positions]))
                 (map #(into %2 {:ting %1}) ting-objs)
                 ;(map #(into %2 {:img (nth isbn-urls %1)}) (range))
                 (map (fn [o] [(:id o) o]))))])

(defn square [a] (* a a))
(defn front-nearest [x y] ; ##
  (:id (apply min-key
    #(+ (square (- x (:x %)))
        (square (- y (:y %))))
    @(subscribe [:front-positions]))))
; ## API-access
(defn cover-api-url [id]
  (str "https://dev.vejlebib.dk/ting-visual-relation/get-ting-object/" id) )
(defn <jsonp [url] ; ### custom jsonp needed due to bug in dev.vejlebib.dk jsonp-implementation
    (let [url (str url "?callback=")
          c (chan)
          id (unique-id)]
      (aset js/window id
            (fn [o]
              (if o 
                (put! c (js->clj o))
                (close! c))
              (goog.object.remove js/window id)))
      (let [tag (js/document.createElement "script")]
        (aset tag "src" (str url id))
        (js/document.head.appendChild tag))
      c))

(defn <search [s] ; ###
  (go (map #(% "_id")
        (get-in (<! (<ajax (str "http://solsort.com/es/bib/ting/_search?q=" s)))
               ["hits" "hits"]))))
;(go (log (<! (<search "harry potter"))))

(defn <info [id] ; ###
  (go (let [o (<! (<ajax (str "http://solsort.com/db/bib/" id)))] 
    {:title (first (o "title"))
     :creator (string/join " & "(o "creator"))
     :related (->> (o "related") (drop 1) (map first))
     :vector (js/Float32Array.from 
               (.map (.split (first (o "vector")) ",") #(js/Number %)))})))
;(go (js/console.log (clj->js (<! (<info "870970-basis:24945669")))))

(defn <cover-url [id] ; ###
  (go (get (first 
             (filter #(= "cover" (get % "property"))
                      (<! (<jsonp (cover-api-url id))))) 
           "value")))
;(go (log (<! (<cover-url "870970-basis:24945669"))))

; ## pointer events
(register-sub
  :pointer-down
  (fn [db]
    (reaction (get-in @db [:pointer :down]))))

(register-handler
  :pointer-up
  (fn [db _]
    (let [oid (get-in db [:pointer :oid])
          book (get-in db [:books oid])
          [x y] (get-in db [:pointer :pos])
          x (- x (.-offsetLeft js/bibappcontainer))
          y (- y (.-offsetTop js/bibappcontainer))
          [x-step y-step] (get db :step-size [1 1])
          x (js/Math.round (/ x x-step))
          y (js/Math.round (/ y y-step))]
      (if book
        (-> db
          (assoc-in [:query]  [:up (:id book)])
          (assoc-in [:release] [x y])
          (assoc-in [:pointer :down] false)
          (assoc-in
            [:books oid]
            (-> book
                (assoc :pos (or (:prev-pos book) (:pos book)))
                (assoc :delta-pos [0 0]))))
        db))))

(register-handler
  :pointer-down
  (fn [db [_ oid x y]]
    (let [book  (get-in db  [:books oid])]
      (log book)
      (-> db
          (assoc-in [:query]  [:down x y oid])
          (assoc-in [:pointer :down] true)
          (assoc-in [:pointer :oid] oid)
          (assoc-in
            [:books oid]
            (-> book
                (assoc :pos :active)
                (assoc :prev-pos (or (:prev-pos book) (:pos book)))))
          (assoc-in [:pointer :pos0] [x y])))))

(register-handler
  :pointer-move
  (fn [db [_ x y]]
    (if (get-in db [:pointer :down])
      (let [[x0 y0] (get-in db [:pointer :pos0])
            [dx dy] [(- x x0) (- y y0)]
            oid (get-in db [:pointer :oid])
            book (get-in db [:books oid])]
        (-> db
            (assoc-in [:query] [:move x y])
            (assoc-in [:pointer :pos] [x y])
            (assoc-in
              [:books oid]
              (-> book
                  (assoc :delta-pos [dx dy])))))
      db)))

(defn pointer-move [e pointer]
  (dispatch-sync [:pointer-move (aget pointer "clientX") (aget pointer "clientY")]) 
  (.preventDefault e))
(defn pointer-down [o e pointer]
  (dispatch-sync [:pointer-down (:id o) (aget pointer "clientX") (aget pointer "clientY")])
  (.preventDefault e))

(defn load-ting [id] ; ##
  (when (not (:title @(subscribe [:ting id])))
    (dispatch-sync [:ting id {:title "[loading]"}])  
    (go (dispatch [:ting id (<! (<info id))]))
    (go (dispatch [:ting id {:cover (<! (<cover-url id))}]))))
(defn book-elem ; ##
  [o x-step y-step]
  (let [[dx dy] (get o :delta-pos [0 0])
        ting @(subscribe [:ting (:ting o)])]
    (load-ting (:ting o))
    [:span
     {:on-mouse-down #(pointer-down o % %)
      :on-touch-start #(pointer-down o % (aget (aget % "touches") 0))
      :style
      (into
        {:background "#333"
         :position :absolute
         :display :inline-block
         :z-index ({:hidden 1 :back 2 :front 3 :saved 4 :active 5} (:pos o))
         :left (+ (* x-step (- (:x o) (/ (:size o) 2))) dx)
         :top (+ (* y-step (- (:y o) (/ (:size o) 2))) dy)
         :width (- (* x-step (:size o)) 1)
         :height (- (* y-step (:size o)) 1)
         :outline (str "1px solid " background-color)}
        (case (:pos o)
          :hidden {}
          :back {}
          :front {:box-shadow "5px 5px 10px black"}
          :saved { :outline "1px solid white" }
          :active{:box-shadow "10px 10px 20px black"}
          (log {}  'ERR-MISSING-POS (:pos o) o) ))}
     [:img {:src (:cover ting) :width "100%" :height "100%"}] 
     [:div {:style {:position "absolute"
                    :display "inline-block"
                    :top 0 :left 0
                    :width "100%" :height "100%"
                    :color "black"
                    :padding 0
                    :margin 0
                    :overflow "hidden"
                    :font-size 10
                    :background
                    (if (= :back (:pos o))
                      "rgba(255,255,255,0.5)"
                      "rgba(0,0,0,0)")}}
      (str 
        ;(:title ting)
        ;ting (:ting o)
        ;(front-nearest (:x o) (:y o))
        
        )
      ]]))

(defn search [] ; ## 
  (log 'search @(subscribe [:query]))
  (go
    (let [results (<! (<search @(subscribe [:query])))]
      (log results))))
(defn bibapp-header [x-step y-step] ; ##
  [:div
   [:div 
    {:on-mouse-down search
     :on-touch-start search
     :style {:display :inline-block
                  :width (* 3 x-step)
                  :text-align "center"
                  :font-size y-step
                  :float "right"
                  :padding-top (* .20 y-step)
                  :padding-bottom (* .20 y-step)
                  :margin (* .20 y-step)
                  :border "1px solid white"
                  :border-radius (* .2 y-step)
                  }}
    "søg"]
   [:input {:value (str @(subscribe [:query]))
            :on-change
            (fn  [e] (dispatch-sync  [:query (-> e .-target  (aget "value"))])) 
            :style {:display :inline-block
                    :width (* 11 x-step)
                    :font-size y-step
                    :padding-top (* .20 y-step)
                    :padding-bottom (* .20 y-step)
                    :margin (* .20 y-step)
                    :background :black
                    :border-top "0px"
                    :border-left "0px"
                    :border-right "0px"
                    :border-bottom "1px solid white"}}]])


(defn bibapp [] ; ##
  (let
    [ww @(subscribe [:width])
     wh @(subscribe [:height])
     xy-ratio (-> (/ (/ wh view-height) (/ ww view-width))
                  (js/Math.min 1.6)
                  (js/Math.max 1.3))
     x-step (js/Math.min
              (/ ww view-width)
              (/ wh view-height xy-ratio))
     y-step (* xy-ratio x-step)]
    (dispatch-sync [:step-size [x-step y-step]])
    (into
      [:div {:on-mouse-move #(pointer-move % %)
             :on-touch-move #(pointer-move % (aget (aget % "touches") 0))
             :on-mouse-up #(dispatch-sync [:pointer-up])  
             :on-touch-end #(dispatch-sync [:pointer-up])  
             :id "bibappcontainer"
             :style {:display :inline-block
                     :width (* x-step view-width)
                     :height (* y-step view-height)
                     :margin-left (* x-step -8)
                     :background background-color
                     :position :absolute
                     :overflow :hidden
                     :color "white"
                     }}
       [bibapp-header x-step y-step]]
      (map #(book-elem % x-step y-step)
           (map second (seq @(subscribe [:books])))))))

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
      "bibapp" {:type :html :html
                [:div
                 {:style
                  {:display :inline-block
                   :position :absolute
                   :width "100%"
                   :height "100%"
                   :text-align :center
                   :background "black"}
                  }
                 [bibapp]]}
      (<default))))

(route "bib" route-fn)
(route "bibdata" route-fn)
