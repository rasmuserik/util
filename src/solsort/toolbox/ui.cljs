(ns solsort.toolbox.ui
  (:require
    [solsort.toolbox.misc :refer [js-seq starts-with]]
    [solsort.toolbox.appdb :as appdb]
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
  (let [status (appdb/db [:loading])]
   (if status
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
               :line-height "48px"
               :text-shadow "2px 2px 8px #000000"
               :padding-bottom (* 0.7 js/window.innerHeight)}}
      (if (string? status) status "Loading...")]
     [:span])))

(defn select [{:keys [db options class style]}]
  (js/console.log 'select style)
  (let [current (appdb/db db)]
    (into [:select
           {:style (into {:padding-left 0
                     :padding-right 0} style)
            :class class
            :value (prn-str current)
            :onChange
            #(appdb/db-async! db (read-string (.-value (.-target %1))))}]
          (for [[k v] options]
            (let [v (prn-str v)]
              [:option {:style {:padding-left 0
                                :padding-right 0}
                        :key v :value v} k])))))
(defn checkbox [{:keys [db]}]
  (let [value (appdb/db db)]
    [:img.checkbox
     {:on-click (fn [] (appdb/db-async! db (not value)) nil)
      :src (if value "assets/check.png" "assets/uncheck.png")}]))

(defn input  [{:keys [type size max-length options db placeholder id style class]
               :or {type "text"
                    style {}}
               :as params}]
  (case type
    :select (select {:db db :options options :style style :class class})
    :checkbox (checkbox {:db db
                         :style style
                         :class class})
    [:input {:type type
             :class class
             :style (into
                     {:padding-right 0
                      :padding-left 0
                      :text-align :center
                      :overflow :visible} style)
             :name (prn-str db)
             :key (prn-str db)
             :id id
             :size size
             :placeholder placeholder
             :max-length max-length
             :value (appdb/db db)
             :on-change #(appdb/db! db(.-value (.-target %1)))}])) 
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
