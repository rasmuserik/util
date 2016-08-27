(ns solsort.toolbox.leaflet
  (:require
   [reagent.core :as reagent]
   [solsort.toolbox.appdb :as appdb :refer [db! db]]
   [solsort.toolbox.misc :refer [canonize-string]]
   [solsort.util :refer [log]]))

(defn cdnjs-img [file]
  (str "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/" file))
(defonce default-marker-icon
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
     :shadowAnchor  [6 42]})))

(defn geo->vec [o] [(.-lat o) (.-lng o)])
(defn vec->geo [o] #js {:lat (first o) :lng (second o)})
(defn openstreetmap
  [o & args]
  (let [id (or (:id o)
               (and (:db o) (prn-str (:db o)))
               (str "leaflet" (.slice (str (js/Math.random)) 2)))
        path (or (:db o) [:solsort-ui id])
        o (into (db path {}) o)
        o (into {:pos (or (:pos0 o) [55.67 12.57])
                 :zoom (or (:zoom0 o) 10)
                 :tile-url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                 :attribution "&copy; OpenStreetMap"} o)
        o (into o {:id id :db path})
        leaflet (atom nil)
        marker-objs (atom [])
        prev-markers (atom [])]
    (db! path o)
    (reagent/create-class
     {:display-name (prn-str path)
      :reagent-render
      (fn [o & args]
        (let [p (db path)
              p (into p o)
              p (assoc p :args args)]
          (when-not (= p (db path))
            (db! path p)))
        [:div {:id id :class (:class o) :style (:style o)}
         "OpenStreetMap " id])
      :component-did-mount
      (fn []
        (let [o (db path)]
          (reset! leaflet (js/L.map id))
          (.setView @leaflet (clj->js (:pos o)) (:zoom o))
          (when (:on-click o)
            (.on @leaflet "click"
                 #((db (conj path :on-click))
                   {:pos (let [ll (aget % "latlng")]
                           [(aget ll "lat")
                            (aget ll "lng")])})))
          (.addTo (js/L.tileLayer
                   (:tile-url o)
                   #js {:attribution (:attribution o)})
                  @leaflet)
          (-> @leaflet .-attributionControl (.setPrefix "Leaflet"))
          (.on @leaflet "moveend"
               #(let [pos (-> % .-target .getCenter)
                      zoom (-> % .-target .getZoom)]
                  (db! path
                       (into (db path)
                             {:pos [(.-lat pos) (.-lng pos)]
                              :zoom zoom})))))
        (js/console.log "did-mount" (js/document.getElementById id)))
      :component-did-update
      (fn [component]
        (let [o (db path)
              map-pos (geo->vec (.getCenter @leaflet))
              pos (:pos o)
              markers (filter #(= :marker (first %)) (:args o))
              markers (map #(assoc (second %) :children (drop 2 %)) markers)]
          (when-not (= pos map-pos)
            (.setView @leaflet (clj->js pos)))
          (when-not (= @prev-markers markers)
            (js/console.log 'update markers @prev-markers)
            (when (< (count @prev-markers) (count markers))
              (doall
               (for [m (drop (count @prev-markers) markers)]
                 (let [marker
                       (js/L.marker
                        (clj->js (:pos m))
                        #js {:icon (or (:icon m) default-marker-icon)})]
                   (swap! marker-objs conj marker)
                   (.addTo marker @leaflet)))))
            (doall
             (map
              (fn [prev current o]
                (when-not (= (:pos prev) (:pos current))
                  (.setLatLng o (clj->js (:pos current)))
                  (.update o))
                #_(log 'change (:pos prev) (:pos current) (geo->vec (.getLatLng o))))
              @prev-markers markers @marker-objs))
            (aset js/window "leaflet" @leaflet)
            (reset! prev-markers markers))))
      :component-will-unmount
      (fn [] (js/console.log "will-unmount"))})))
#_(defn- openstreetmap-inner
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
          marker-objs (atom [])
          marker-cluster (atom nil)]
      (reagent/create-class
       {:display-name id
        :reagent-render
        (fn []
          [:div {:id id :class class :style style}
           "OpenStreetMap" id])
        :component-did-mount
        (fn []
          (when (js/document.getElementById id)
            (reset! leaflet (js/L.map id))
            (reset! marker-cluster (js/L.markerClusterGroup))
            (when on-click
              (.on @leaflet "click"
                   #(on-click {:pos (let [ll (aget % "latlng")]
                                      [(aget ll "lat")
                                       (aget ll "lng")])})))
            (.setView @leaflet (clj->js pos) zoom)
            (.addTo (js/L.tileLayer tile-url #js {:attribution attribution}) @leaflet)
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
            (.addLayer @leaflet @marker-cluster)))
        :component-did-update
        (fn [component]
          (when (js/document.getElementById id)
            (let [map-pos (geo->vec (.getCenter @leaflet))
                  pos (appdb/db (conj db :pos))
                  prev-markers (appdb/db (conj db :marker-cache))
                  markers (filter #(= :marker (first %)) (appdb/db (conj db :args)))
                  markers (map #(assoc (second %) :children (drop 2 %)) markers)]
              (when-not (= pos map-pos)
                (.setView @leaflet (clj->js pos)))
              (when-not (= prev-markers markers)
                #_(log 'update markers prev-markers)
            ; todo remove marker
                (when (< (count prev-markers) (count markers))
                  (doall
                   (for [m (drop (count prev-markers) markers)]
                     (let [marker
                           (js/L.marker
                            (clj->js (:pos m))
                            #js {:icon (marker-icons (or (:type m) :default))})]
                       (swap! marker-objs conj marker)
                       (.addTo marker @leaflet)))))
                (doall
                 (map
                  (fn [prev current o]
                    (when-not (= (:pos prev) (:pos current))
                      (.setLatLng o (clj->js (:pos current)))
                      (.update o))
                    #_(log 'change (:pos prev) (:pos current) (geo->vec (.getLatLng o))))
                  prev-markers markers @marker-objs))
                (aset js/window "leaflet" @leaflet)
                (appdb/db! (conj db :marker-cache) markers))

              #_(log 'component-update component map-pos pos args markers))))
        :component-will-unmount
        (fn [] (when gc (appdb/db! db)))})))
#_(defn ^:export xopenstreetmap [{:keys [marker-icons db gc pos pos0 zoom zoom0 id]
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
        (appdb/db! (conj newdb :args) args)
        [openstreetmap-inner (into (appdb/db newdb o) params)])))
