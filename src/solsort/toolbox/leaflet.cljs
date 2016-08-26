(ns solsort.toolbox.leaflet
  (:require
   [reagent.core :as reagent]
   [solsort.toolbox.appdb :as appdb]
   [solsort.toolbox.misc :refer [canonize-string]]
   [solsort.util :refer [log]]))

(defn cdnjs-img [file]
  (str "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/" file))
(defonce default-marker-icons
  {:default
   (js/L.icon
    (clj->js
     {:iconUrl (cdnjs-img "marker-icon.png")
      :iconRetinaUrl (cdnjs-img "marker-icon-2x.png")
      :iconSize  [25 41]
      :iconAnchor  [12 38]
      :popupAnchor  [-3 -76]
      :shadowUrl (cdnjs-img "marker-shadow.png")
      :shadowRetinaUrl (cdnjs-img "marker-shadow.png")
      :shadowSize  [25 45]
      :shadowAnchor  [6 42]}))})

(defn geo->vec [o] [(.-lat o) (.-lng o)])
(defn vec->geo [o] #js{:lat (first o) :lng (second o)})
(defn- openstreetmap-inner
  [{:keys [db pos zoom markers tile-url attribution handler class gc
           marker-icons on-click id style]
    :or {pos [51.505 -0.09]
         scale 13
         markers []
         marker-icons default-marker-icons
         tile-url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
         attribution "&copy; OpenStreetMap"}
    :as o}]
  (let [leaflet (atom nil)
        marker-cluster (atom nil)]
    (reagent/create-class
     {:display-name id
      :reagent-render
      (fn [] [:div {:id id :class class :style style}
              "OpenStreetMap" id])
      :component-did-mount
      (fn []
        (reset! leaflet (js/L.map id))
        (reset! marker-cluster (js/L.markerClusterGroup))
        (when on-click
          (.on @leaflet "click" #(on-click {:pos (let [ll (aget % "latlng")]
                                                   [(aget ll "lat")
                                                    (aget ll "lng")])})))
        (.setView @leaflet (clj->js pos) zoom)
        (.addTo (js/L.tileLayer tile-url #js {:attribution attribution})
                @leaflet)
        (-> @leaflet .-attributionControl (.setPrefix "Leaflet"))
        (.on @leaflet "moveend"
             #(let [pos (-> % .-target .getCenter)
                    zoom (-> % .-target .getZoom)]
                (appdb/db!
                 db
                 (-> o
                     (assoc :pos [(.-lat pos) (.-lng pos)])
                     (assoc :zoom zoom)))))
        (doall
         (for [m markers]
           (let [marker
                 (js/L.marker
                  (clj->js (:pos m))
                  #js {:icon (marker-icons (or (:type m) :default))})]
             (when (:click m) (.on marker "click" (:click m)))
             (.addLayer @marker-cluster marker))))
        (.addLayer @leaflet @marker-cluster))
      :component-did-update
      (fn [component]
        (let [map-pos (geo->vec (.getCenter @leaflet))
              pos (appdb/db (conj db :pos))]
          (when-not (= pos map-pos)
            (.setView @leaflet (clj->js pos))
            )
          #_(log 'component-update component map-pos pos)))
      :component-will-unmount
      (fn [] (when gc (appdb/db! db)))})))

(defn ^:export openstreetmap [{:keys [marker-icons db gc pos pos0 zoom zoom0 id]
                               :as params}]
  (let [newdb (or db
                  (and id [:solsort-ui id])
                  [:solsort-ui (str "leaflet" (.slice  (str  (js/Math.random)) 2))])
        newdb (if-not (coll? newdb) [db] newdb)
        orig (appdb/db newdb)
        pos (or pos (:pos orig) pos0)
        id (or id (canonize-string (prn-str newdb)))
        o {:db newdb
           :id id
           :marker-icons (or marker-icons default-marker-icons)
           :gc (if db gc true)
           :pos (if (seqable? pos) pos [55.67 12.57])
           :zoom (or zoom (:zoom orig) zoom0 10)}
        o (into params o)]
    (appdb/db-async! newdb o)
    (fn [params & args]
      (log 'fn (:pos (second (first args))))
      [openstreetmap-inner (into (appdb/db newdb o) params)])))
