(ns solsort.ui
  (:require
    [solsort.misc :refer [js-seq starts-with]]
    [reagent.core :as reagent :refer  []]))

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

