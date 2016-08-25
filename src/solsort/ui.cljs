(ns solsort.ui
  (:require
    [solsort.misc :refer [js-seq starts-with]]
    [solsort.appdb :as appdb]
    [reagent.core :as reagent :refer []]
    [cljs.reader :refer [read-string]]))

(defn html-data [elem]
  (into {} (->> (js-seq (.-attributes elem))   
                (map (fn [attr] [(.-name attr) (.-value attr)]))  
                (filter (fn [[k w]] (starts-with k "data-")))
                (map (fn [[k w]] [(.slice k 5) w])))))

(defn page-ready [] (js/setTimeout #((aget js/window "onSolsortReady")) 20))

(defn render [o]
  (when-not (js/document.getElementById "main")
    (js/document.body.appendChild 
      (doto (js/document.createElement "div")
        (aset "id" "main"))))
 (reagent/render-component o  (js/document.getElementById "main")))

(defn loading "simple loading indicator, showing when (appdb/db [:loading])" []
  (if (appdb/db [:loading])
    [:div
     {:style {:position :fixed
              :display :inline-block
              :top 0 :left 0
              :width "100%"
              :heigth "100%"
              :background-color "rgba(0,0,0,0.6)"
              :color "white"
              :z-index 100
              :padding-top (* 0.3 js/window.innerHeight)
              :text-align "center"
              :font-size "48px"
              :text-shadow "2px 2px 8px #000000"
              :padding-bottom (* 0.7 js/window.innerHeight)}}
     "Loading..."]
    [:span]))
(defn select [id options]
  (let [current (appdb/db id)]
    (into [:select
           {:style {:padding-left 0
                    :padding-right 0}
            :value (prn-str current)
            :onChange
            #(appdb/db-async! id (read-string (.-value (.-target %1))))}]
          (for [[k v] options]
            (let [v (prn-str v)]
              [:option {:style {:padding-left 0
                                :padding-right 0}
                        :key v :value v} k])))))
(defn checkbox [id]
  (let [value (appdb/db id)]
    [:img.checkbox
     {:on-click (fn [] (appdb/db-async! id (not value)) nil)
      :src (if value "assets/check.png" "assets/uncheck.png")}]))
(defn input  [id & {:keys [type size max-length options]
                    :or {type "text"}}]
  (case type
    :select (select id options)
    :checkbox (checkbox id)
    [:input {:type type
             :style {:padding-right 0
                     :padding-left 0
                     :text-align :center
                     :overflow :visible}
             :name (prn-str id)
             :key (prn-str id)
             :size size
             :max-length max-length
             :value (appdb/db id)
             :on-change #(appdb/db! id (.-value (.-target %1)))}])) 
(defn- fix-height "used by rot90" [o]
  (let [node (reagent/dom-node o)
        child (-> node (aget "children") (aget 0))
        width (aget child "clientHeight")
        height (aget child "clientWidth")
        style (aget node "style")]
    (aset style "height" (str height "px"))
    (aset style "width" (str width "px"))))
(def rot90 "reagent-component rotating its content 90 degree"
  (with-meta
    (fn [elem]
      [:div
       {:style {:position "relative"
                :display :inline-block}}
       [:div
        {:style {:transform-origin "0% 0%"
                 :transform "rotate(-90deg)"
                 :position "absolute"
                 :top "100%"
                 :left 0
                 :display :inline-block}}
        elem]])
    {:component-did-mount fix-height
     :component-did-update fix-height}))
