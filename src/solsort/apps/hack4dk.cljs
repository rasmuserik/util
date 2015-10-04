(ns solsort.apps.hack4dk
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])
  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]

    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch dispatch-sync]]

    [solsort.util :refer [route log unique-id <p]]
    [solsort.misc :refer [<seq<! js-seq]]
    [clojure.string :refer [replace split blank?]]
    [solsort.net :refer [<ajax]]
    [solsort.ui :refer [app input default-shadow add-style icon]]
    ))

(route
  "natmusapi-proxy"
  (fn [o]
    (go (let [url (replace (o "url")
                           #"^.?natmusapi-proxy"
                           "http://testapi.natmus.dk")]
          {:type :json :json (<! (<ajax url :result :json))}))))

;; # 360-natmus
;(comment js/console.log 
;        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=(categories:Rotationsbilleder)" )))
;      (js/console.log 
;        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=(sourceId:106977)" ))))
;      (js/console.log 
;        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=(sourceId:106677)" ))))
;      (js/console.log 
;        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=solvogn* and (categories:Rotationsbilleder)" )))))

; TODO reverse rotation direction
; collection on image id
(defn <natmus-id [id]
  (let [[_ collection sourceId] (re-matches #"([^/]*)/(.*)" id)]
  (log id sourceId collection)
  (<ajax (str 
           "//testapi.natmus.dk"
           "/v1/search/?query=(sourceId:" sourceId ") " 
           "AND (collection:" collection ")") 
         :credentials false)))

(defn <natmus-images [id]
  (go (let [imgs (->> (get (<! (<natmus-id id)) "Results")
                      (map #(get % "relatedSubAssets"))
                      (filter #(< 0 (count %)))
                      (first)
                      (map #(<natmus-id 
                              (str (get % "collection") 
                                  "/" (get % "sourceId"))
                                        ; also get "collection"
                                        ; query should be (sourceId:..) AND (collection:...)
                                        )))
            imgs (map #(get % "Results") (<! (<seq<! imgs)))
            imgs (map (fn [o] (->> o
                                   (map #(get % "assetUrlSizeMedium"))
                                   (filter #(< 0 (count %)))
                                   (first)))
                      imgs)]
        (or imgs []))))

(register-handler 
  :360-img-load
  (fn  [db  [_ oid url]]  
    (update-in db [:360 oid "loaded"] #(conj (or % #{}) url))))

(register-handler 
  :360-images 
  (fn  [db  [_ oid imgs]]  
    (assoc-in db [:360 oid :imgs] imgs)))

(register-handler 
  :360-pos
  (fn  [db  [_ pid pos]]  (assoc-in db [:360-widget pid :pos] pos)))

(register-sub :360-images (fn [db [_ oid]] (reaction (get-in @db [:360 oid] {}))))
(register-sub :360-widget (fn [db [_ pid]] (reaction (get-in @db [:360-widget pid] {}))))

(defn handle-move [pid e]
  (let [client-x (aget e "clientX")
        target (aget e "target")
        target-width  (aget target "offsetWidth")]
    (dispatch [:360-pos pid (/ client-x target-width)])))

(defn view-360 [pid oid]
  (let [o @(subscribe [:360-images oid])
        widget @(subscribe [:360-widget pid])]
    (if-not (:imgs o)
      [:div "[... getting data / images, can take a while ...]"]
      (let [imgs (:imgs o)
            img-count (count imgs)
            pos (-> (get widget :pos 0)
                    (mod 1)
                    (* (count imgs))
                    (js/Math.floor))
            ]

        [:div
        [:img {:src (nth imgs pos)
               :on-mouse-move (partial handle-move pid)
               :width "100%"}] 
         (into 
           [:div {:style {:display "none" }}]
           (map
             (fn [src]
               [:img {:src src
                :width (str (/ 100 img-count) "%")}])
             imgs)
          )
         ]
        
        ))))

(route "360"
       (fn [o]
         (go
           (let [obj-id (o "src")
                 [_ src id] (re-find #"^([^:]*):(.*)$" obj-id)
                 pid (:id o)]
             (go
               (dispatch 
                 [:360-images obj-id 
                  (case src "natmus" (<! (<natmus-images id)) [])]))
             {:type :html
              :html [view-360 pid obj-id]}))))
;; # filmografi
(defn name->kw [o] (keyword (str (.-nodeName o))))
(defn dom->clj [dom]
  (case (.-nodeType dom)
    ((.-DOCUMENT_NODE dom) (.-ELEMENT_NODE dom)) 
    (let [tag (name->kw dom)
          children (map dom->clj (js-seq (.-children dom))) 
          children (if (empty? children)
                     (if (blank? (.-textContent dom))
                       []
                       [(str (.-textContent dom))])
                     children)
          attrs (into {} (map (fn [o] [(name->kw o) (.-textContent o)]))
                      (js-seq (or (.-attributes dom) [])))]
      {:tag tag
       :attrs attrs
       :children children})
    (.-TEXT_NODE dom) (str (.-textContent dom))))

(defn parse-xml [s] (dom->clj (.parseFromString (js/DOMParser.) s "text/xml")))
(defn parse-html [s] (dom->clj (.parseFromString (js/DOMParser.) s "text/html")))

(defn <film-page [n]
  (go (let [url (str "http://nationalfilmografien.service.dfi.dk"
                     "/movie.svc/list?startrow=" n "00&rows=100")
            xml (parse-xml (<! (<ajax url :result :text)))]
        [:div (map (fn [o] [:div 
                            (-> o :children (nth 0) :children first)
                            (-> o :children (nth 1) :children first)])
                   (:children (first (:children xml))))])))

(defn film-page-list []
  (into
    [:div]
    (interpose " "
               (map 
                 (fn [i]
                   [:a {:href (str js/location.href "/pages/" i)
                        :style {:margin ".5ex"}
                        } i])
                 (take 232 (range))))))

(route 
  "filmografi"
  (fn [o]
    (go (let [path  (split  (o "path") "/")]
          (log path (second path))
          {:type :html
           :html 
           (case (second path)
             "pages" (<! (<film-page (nth path 2)))
             "" [film-page-list])}))))
