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
(defn jslog [o] (js/console.log (clj->js o)) o)
(defn square [a] (* a a))
(defn epsilon [] (* 0.00001 (- (js/Math.random) (js/Math.random))))
(def background-color "black")
(def header-space 2)
(def view-width 16)
(def view-height (+ header-space 18))
(def widget-height (- view-height header-space 2.5))
(def isbn-covers true)

(defonce ting-objs  ; ##
  (cycle
    (shuffle 
      ["870970-basis:29820031" "870970-basis:45231402" "870970-basis:29146004" 
       "870970-basis:28794630" "870970-basis:28904061" "870970-basis:45574881" 
       "870970-basis:51604288" "870970-basis:44351641" "870970-basis:45470075" 
       "870970-basis:27697917" "870970-basis:22324284" "870970-basis:28452551" 
       "810010-katalog:008471560" "870970-basis:44741830" "870970-basis:28534698" 
       "870970-basis:45583457" "870970-basis:45386864" "870970-basis:45421716" 
       "870970-basis:28052472" "870970-basis:45493016" "870970-basis:44291738" 
       "870970-basis:23060132" "810010-katalog:007071351" "870970-basis:45554813" 
       "870970-basis:45237648" "870970-basis:28407513" "870970-basis:44950723" 
       "830380-katalog:93161505" "870970-basis:27006434" "870970-basis:45618765" 
       "870970-basis:26666074" "870970-basis:44695634" 
       "870970-basis:27455344" "870970-basis:28815263" "870970-basis:27578381" 
       "870970-basis:50914968" "870970-basis:45170306" "870970-basis:45233758" 
       "870970-basis:29706328" "870970-basis:51582772" "870970-basis:45199088" 
       "870970-basis:27880436" "870970-basis:29991537" "870970-basis:44313235" 
       "870970-basis:23116642" "870970-basis:45233332" "870970-basis:44547759" 
       "870970-basis:44910888" "870970-basis:51313380" "870970-basis:44887509" 
       "870970-basis:26829798" "870970-basis:45005801" "870970-basis:25893018" 
       "870970-basis:44364999" "870970-basis:44331225" "870970-basis:50625656" 
       "870970-basis:45534952" "870970-basis:44591413" "870970-basis:44592045" 
       "870970-basis:28522517" "870970-basis:29100160" "870970-basis:26396417" 
       "870970-basis:50565858" "870970-basis:28930240" "870970-basis:28108990" 
       "870970-basis:27195105" "870970-basis:28372531" "870970-basis:44831562" 
       "870970-basis:50520846" "870970-basis:45182266" "870970-basis:29158746" 
       "870970-basis:43917579" "870970-basis:45217345" "870970-basis:45263762" 
       "870970-basis:50909794" "810010-katalog:007144163" "870970-basis:26952425" 
       "870970-basis:27873251" "870970-basis:45350568" "870970-basis:44850001" 
       "870970-basis:44520028" "870970-basis:44150484" "870970-basis:27561527" 
       "870970-basis:27867138" "870970-basis:28539290" "870970-basis:45153843" 
       "870970-basis:29287341" "870970-basis:26681316" "870970-basis:45281434" 
       "870970-basis:28715730" "870970-basis:45300439" "870970-basis:45575969" 
       "870970-basis:28283032" "870970-basis:28379129" 
       "870970-basis:27374859" "820010-katalog:3096314" "870970-basis:26509904" 
       "870970-basis:44741385" "870970-basis:28958188" 
       "870970-basis:44406365" "870970-basis:44623234" "870970-basis:44973650" 
       "870970-basis:44537052" "870970-basis:51283708" "870970-basis:45377458" 
       "870970-basis:28009011" "870970-basis:45076261" "870970-basis:27165435" 
       "870970-basis:24232123" "870970-basis:45164683" "870970-basis:44529807"])))


; ## subscriptions: :books :back-positions :front-positions :saved-positions :step-size :query
(register-sub :books (fn [db] (reaction (get @db :books {}))))
(register-handler :books (fn [db [_ books]] (assoc db :books (into (get db :books {}) books))))

(register-sub :back-positions (fn [db] (reaction (get @db :back-positions []))))
(register-handler :back-positions 
                  (fn [db [_ back-positions]] (assoc db :back-positions back-positions)))

(register-sub :front-positions (fn [db] (reaction (get @db :front-positions []))))
(register-handler :front-positions 
                  (fn [db [_ front-positions]] (assoc db :front-positions front-positions)))

(register-sub :query (fn [db] (reaction (get @db :query))))
(register-handler :query (fn [db [_ q]] (assoc db :query q)))

(register-sub :coverable (fn [db] (reaction (get @db :coverable))))
(register-handler :coverable (fn [db [_ coverable]] (assoc db :coverable coverable)))

(register-sub :show (fn [db] (reaction (get @db :show))))

(register-sub :step-size (fn [db] (reaction (get @db :step-size))))
(register-handler :step-size (fn [db [_ step-size]] (assoc db :step-size step-size)))

(register-sub 
  :ting 
  (fn [db [_ id]] 
    (reaction (get-in @db [:ting id] {}))))
(register-handler 
  :ting (fn [db [_ id o]] 
          (assoc-in db [:ting id] (into (get-in db [:ting id] {}) o))))

; ## :front-positions :back-positions :books initialisation
(defn set-id [type os] 
  (map #(into %1 {:id [type %2] :x (+ (:x %1) (epsilon)) :y (+ (:y %1) (epsilon))}) 
       os (range)))

(dispatch-sync
  [:front-positions
   (set-id :front
           (concat
             (map #(into % {:y (+ header-space (:y %)) :size 3 :pos :front})
                  #_[{:x 3 :y 2} {:x 13 :y 2}
                   {:x 8 :y 6} 
                   {:x 8 :y 12}
                   {:x 2 :y 9} {:x 14 :y 9}
                   {:x 3 :y 16} {:x 13 :y 16}]
                  [{:x 2 :y 3} {:x 10 :y 3}
                   {:x 6 :y 7} {:x 14 :y 7}
                   {:x 2 :y 11} {:x 10 :y 11}
                   {:x 6 :y 15} {:x 14 :y 15} ]
                  #_[{:x 2 :y 2} {:x 10 :y 2}
                   {:x 6 :y 5} {:x 14 :y 5}
                   {:x 2 :y 8} {:x 10 :y 8}
                   {:x 6 :y 11} {:x 14 :y 11}
                   {:x 2 :y 14} {:x 10 :y 14}]
                  #_(map (fn [t] {:x (+ 8 (* 6 (js/Math.sin t))) 
                                :y (+ 9 (* -7 (js/Math.cos t)))})
                       (range 0 (* 2 js/Math.PI) (/ js/Math.PI 4))))
             #_(map (fn [x] {:x x :y (- view-height 1) :size 1.8 :pos :saved}) 
                    (range 1 17 2))))])

(dispatch-sync 
  [:back-positions
   (map 
     (fn [o]
       ; TODO: have list of nearest 3 front-neighbours, instead of single
       (assoc o :front-neighbours
              [(:id
                 (apply min-key
                        #(+ (square (- (:x o) (:x %)))
                            (square (- (:y o) (:y %))))
                        @(subscribe [:front-positions])))]))
     (set-id :back
             (map (fn [x y] {:x x :y (+ y header-space) :size 2 :pos :back})
                  (cycle (concat (range 1 17 2) (range 0 17 2)))
                  (concat (repeat 8 1) (repeat 9 3)
                          (repeat 8 5) (repeat 9 7)
                          (repeat 8 9) (repeat 9 11)
                          (repeat 8 13) (repeat 9 15)
                          (repeat 8 17) 
                          ))))])

(defonce books-initialise
  (dispatch-sync
    [:books
     (into {} (->> 
                (concat @(subscribe [:front-positions]))
                (map #(into %2 {:ting %1}) ting-objs)
                (map (fn [o] [(:id o) o]))))]))

(defn back-books [db] ; ##
  (assoc 
    db :books
    (loop [backs (:back-positions db)
           taboo 
           (into #{} (concat 
                       (->> (:books db) 
                            (map second) 
                            (filter #(= :front (first (:id %)))) 
                            (map :ting))
                       #_(->> (:ting db)
                              (filter (fn [[a b]] (not (:has-cover b))))
                              (map first))
                       ))
           books (:books db)

           ]
      (if (seq backs)
        (let [o (first backs)
              parent-id (first (:front-neighbours o))
              parent (get-in db [:books parent-id])
              options (filter #(not (taboo %))
                              (get-in db [:ting (:ting parent) :related]))   
              options (filter #(contains? (:coverable db) %) options)
              ting (first options)
              books (assoc books (:id o) (into o {:ting ting}))]
          (recur (rest backs) (conj taboo ting) books))
        books))))
(register-handler :back-books back-books)
(dispatch-sync [:back-books])
; ## API-access
(defn cover-api-url [id]
  (str "https://dev.vejlebib.dk/ting-visual-relation/get-ting-object/" id))
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
           (get-in (<! (<ajax (str "http://solsort.com/es/bibapp/ting/_search?q=" s)))
                   ["hits" "hits"]))))
;(go (log (<! (<search "harry potter"))))

(defn <info [id] ; ###
  (go (let [o (<! (<ajax (str "http://solsort.com/db/bib/" id)))] 
        {:title (first (o "title"))
         :description (first (o "description"))
         :abstract (first (o "abstract"))
         :date (first (o "date"))
         :creator (string/join " & "(o "creator"))
         :related (->> (o "related") (drop 1) (map first))
         :isbn (->> (o "isbn") (filter #(= "978" (.slice % 0 3))) (first))
         :isbn-cover (->> (o "isbn")
                          (filter #(= "978" (.slice % 0 3)))
                          (map #(str "http://bogpriser.dk/Covers/"  (.slice % 10) "/" % ".jpg"))
                          (first))
         :has-cover (first (o "hasTingCover"))
         :vector (js/Float32Array.from 
                   (.map (.split (first (o "vector")) ",") #(js/Number %)))})))
;(go (js/console.log (clj->js (<! (<info "870970-basis:24945669")))))

(defn <cover-url [id] ; ###
  (go (get (first 
             (filter #(= "cover" (get % "property"))
                     (<! (<jsonp (cover-api-url id))))) 
           "value")))
;(go (log (<! (<cover-url "870970-basis:24945669"))))

(defn find-nearest [db [x y]] ; ##
  (:id (apply min-key
              #(+ (square (- x (:x %)))
                  (square (- y (:y %))))
              (:front-positions db))))

(defn calc-back [db] ; ##
  (let [back-pos (:back-positions db)])
  )
(register-handler :calck-back (fn [db _] (calc-back db)))
; ## pointer events
(register-sub :pointer-down (fn [db] (reaction (get-in @db [:pointer :down]))))

(defn pos-obj [db [type id]]
  (nth (if (= type :front) (db :front-positions) (db :back-positions)) id))


(defn release [db oid book [x y]]
  (let [nearest (find-nearest db [x y])
        nearest-book (get-in db [:books nearest])
        [dx dy] (get book :delta-pos)
        max-dist (* 0.5 (:size nearest-book))
        overlap (if (and (> max-dist (js/Math.abs (- x (:x nearest-book)))) 
                         (> max-dist (js/Math.abs (- y (:y nearest-book))))
                         (not= oid nearest)

                         )
                  nearest
                  nil)
        db (assoc-in db [:books oid] (assoc (pos-obj db oid) :ting (:ting book)))]
    (cond
      overlap 
      (-> db 
          (assoc-in [:books overlap] (assoc (pos-obj db overlap) :ting (:ting book)))
          (assoc-in [:books oid] (assoc (pos-obj db oid) :ting (:ting nearest-book))))

      (and (> 100 (+ (* dx dx) (* dy dy)))
           (> 1500 (- (js/Date.now) (get-in db [:pointer :start-time]))))
      (assoc-in db [:show] (get-in db [:books oid :ting]))

      :else db)))

(register-handler
  :pointer-up
  (fn [db _]
    (if-not (get-in db [:pointer :down])
      db
      (let [oid (get-in db [:pointer :oid])
            book (get-in db [:books oid])
            start-time (get-in db [:pointer :start-time])
            [x y] (get-in db [:pointer :pos])
            x (- x (.-offsetLeft js/bibappcontainer))
            y (- y (.-offsetTop js/bibappcontainer))
            [x-step y-step] (get db :step-size [1 1])
            [x y] [(/ x x-step) (/ y y-step)]
            db (assoc-in db [:pointer :down] false)]
        (if book
          (-> db
              (release oid book [x y]) 
              (back-books))
          db)))))

(register-handler
  :pointer-down
  (fn [db [_ oid x y]]
    (if (get db :show)
      (assoc db :show nil)
      (let [book  (get-in db  [:books oid])]
        (-> db
            (assoc-in [:pointer :start-time] (js/Date.now))
            (assoc-in [:pointer :down] true)
            (assoc-in [:pointer :oid] oid)
            (assoc-in
              [:books oid]
              (-> book
                  (assoc :pos :active)
                  (assoc :prev-pos (or (:prev-pos book) (:pos book)))))
            (assoc-in [:pointer :pos] [x y])
            (assoc-in [:pointer :pos0] [x y]))))))

(register-handler
  :pointer-move
  (fn [db [_ x y]]
    (if (get-in db [:pointer :down])
      (let [[x0 y0] (get-in db [:pointer :pos0])
            [dx dy] [(- x x0) (- y y0)]
            oid (get-in db [:pointer :oid])
            book (get-in db [:books oid])]
        (-> db
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
    (go (let [o (<! (<info id))]
          (dispatch [:ting id o])
          (dispatch [:ting id {:cover (if (contains? @(subscribe [:coverable]) id)  
                                        (:isbn-cover o)
                                        (<! (<cover-url id)))} ])
          (dispatch [:back-books])))
    ))
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
          :front {:box-shadow "5px 5px 10px black" }
          :saved { :outline "1px solid white" }
          :active{:box-shadow "10px 10px 20px black"}
          (log {}  'ERR-MISSING-POS (:pos o) o) ))}
     [:img {:src (or (:isbn-cover ting) (:cover ting)) :width "100%" :height "100%"}] 
     [:div {:style {:position "absolute"
                    :display "inline-block"
                    :top 0 :left 0
                    :width "100%" :height "100%"
                    ;:color "black"
                    :color "#333"
                    :padding 0
                    :margin 0
                    :overflow "hidden"
                    :font-size 10
                    :background
                    (if (= :back (:pos o))
                      "rgba(255,255,255,0.45)"
                      "rgba(0,0,0,0)")}}

      ;(:title ting)
      ]]))

(defn search [] ; ## 
  (go
    (let [results (<! (<search @(subscribe [:query])))
          positions 
          (->> @(subscribe [:front-positions])
               (filter #(= :front (:pos %)))
               )
          books 
          (map 
            (fn [id o] [(:id o) (into o {:ting id})]) 
            results 
            (shuffle positions))
          ]
      (dispatch [:books books])
      (dispatch [:back-books]))))
(defn bibapp-header [x-step y-step] ; ##
  [:div
   [:input
    {:type "submit"
     :on-mouse-down search
     :on-touch-start search
     :on-submit search
     :value "søg"
     :style {:display :inline-block
             :width (* 3.5 x-step)
             :text-align "center"
             :background "black"
             :font-size y-step
             :float "right"
             :padding-top (* .20 y-step)
             :padding-bottom (* .20 y-step)
             :margin (* .20 y-step)
             :border "2px solid white"
             :border-radius (* .2 y-step)
             }}]
   [:input {:value (str @(subscribe [:query]))
            :on-key-down #(when (= 13 (.-keyCode %)) (search))
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
                    :border-bottom "2px solid white"}}]])


(defn bibinfo [] ; ##
  (if @(subscribe [:show]) 
    (let [id @(subscribe [:show])    
          o @(subscribe [:ting id])]
      [:div {
             :on-mouse-down #(pointer-down o % %)
             :on-touch-start #(pointer-down o % (aget (aget % "touches") 0))
             :style 
             {:position :absolute
              :top 0
              :left "3%"
              :width "94%"
              :max-height "96%"
              :overflow :hidden
              :padding "0px"
              :box-sizing :border-box
              :box-shadow "5px 5px 10px black"
              :outline "1px solid black"
              ;:border "2px solid black"
              :color "black"
              ;:border-radius "3px"
              :text-align :left
              :background "rgba(255,245,230,0.9)"
              :text-shadow "0px 0px 4px white"
              :z-index "6" }}


       [:h1 {:style {:clear :none :text-align :center}} (:title o)]
       [:img {:src (:cover o) 
              :style
              {:width "50%"
               :float :right
               :margin-bottom "2%"
               }
              }]
       [:div {:style 
              {
               :text-align :center}}[:i "af " (:creator o)]]
       [:p {:style 
            {:text-shadow "none"
             :font-size "80%"
             :text-align :center
             }} 
        [:a {:href 
             (str "http://bibliotek.dk/linkme.php?rec.id=" id)
             :target "_blank"
             :on-mouse-down #(js/open (str "http://bibliotek.dk/linkme.php?rec.id=" id))
             :on-touch-start #(js/open (str "http://bibliotek.dk/linkme.php?rec.id=" id))
             :style
             {:display :inline-block
              :box-sizing :border-box
              :font-weight :bold
              :text-decoration "none"
              :color "white"
              :background "black"
              :padding "8px 2px 8px 4px"}}
         " BIBLIOTEK" [:span 
                       {:style {:color "#088eb4"}}
                       "DK "]]
        [:a {:href 
             (str "https://bibliotek.dk/da/reservation?ids=" id)
             :on-mouse-down #(js/open (str "https://bibliotek.dk/da/reservation?ids=" id))
             :on-touch-start #(js/open (str "https://bibliotek.dk/da/reservation?ids=" id))
             :target "_blank"
             :style
             {:display :inline-block
              :box-sizing :border-box
              :text-decoration "none"
              :background "#088eb4"
              :font-weight :bold
              :color "white"
              :padding "6px 2px 6px 2px" 
              :border-left "1px solid white"
              :border-top "2px solid #088eb4"
              :border-right "2px solid #088eb4"
              :border-bottom "2px solid #088eb4"}}
         " Bestil "]
        [:p {:style 
            {:text-shadow "none"
             :font-size "60%"
             :text-align :center
             }} 
         [:a {:href 
             (str "http://www.bogpriser.dk/Search/Result?isbn=" (:isbn o)) 
             :target "_blank"
             :on-mouse-down #(js/open (str "http://www.bogpriser.dk/Search/Result?isbn=" (:isbn o)))
             :on-touch-start #(js/open (str "http://www.bogpriser.dk/Search/Result?isbn=" (:isbn o)))
             :style
             {:display :inline-block
              :box-sizing :border-box
              :font-weight :bold
              :text-decoration "none"
              :color "white"
              :background "#605746"
              :padding "7px 7px 7px 7px"}}
         " BOGPRISER" [:span 
                       {:style {:color "#ffdc12"}}
                       ".DK "]]]
        
        ]
       [:p 
        {:style {:margin "5%" :hyphens "auto" }}
        (or (:abstract o) (:description o))]
       [:hr]
       [:p 
        {:style {:margin "5%"}}
        "Udgivet " (:date o)]])
    [:span]))
(defn splash-screen [] ; ##
  [:div
   {:style {:color "#cfc"}}
   [:h1 "BibApp"]
   [:h2 "Eksperimentel prototype"]
   [:p "- ikke optimeret, så hav tålmodighed."]
   [:br] [:br] [:br] [:br]
   [:p "solsort.com"]]
  )

         (defn bibfooter []; ##
           [:div
          {:style
           {:position :absolute
            :z-index 6
            :width "96%"
            :bottom 0
            :font-size 12
            :margin "2%"

            :text-shadow "
            0px 0px 1px black,
            0px 0px 2px black,
            0px 0px 3px black,
            0px 0px 3px black,
            0px 0px 2px black,
            0px 0px 1px black
                         "
            :height 24
            :color "#dfd"
            }
           }
          #_[:div {:style
                 {:display :inline-block
                  :float :left
                  :font-weight :bold
                  :text-align :left} }
          "BibApp" [:br]
          "solsort.com"]
          [:div {:style
                 {:display :inline-block
                  :float :right
                  :text-align :right } }
          " Eksperimentel prototype," [:br]
          "- ikke optimeret, så hav tålmodighed."]
          ])
          
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
     y-step (* xy-ratio x-step)
   ; x-step 20
   ; y-step 30
    ]
    (dispatch-sync [:step-size [x-step y-step]])
    (if @(subscribe [:coverable]) 
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
         [bibapp-header x-step y-step]
         [bibinfo]
         [bibfooter]
         ]
        (map #(book-elem % x-step y-step)
             (map second (seq @(subscribe [:books])))))
      (do
        (go
          (dispatch [:coverable (into #{} (get (<! (<ajax "http://solsort.com/db/bib/coverable")) "coverable"))])
          )
        [splash-screen]
        ))))

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
