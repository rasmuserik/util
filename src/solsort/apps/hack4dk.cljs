(ns solsort.apps.hack4dk
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])
  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]

    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch dispatch-sync]]

    [solsort.util :refer [route log unique-id <p]]
    [solsort.misc :refer [<seq<!]]
    [clojure.string :refer [replace split]]
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
; non-cors-credentials
; collection on image id
(defn <natmus-id [id]
  (<ajax (str 
           "//blog.solsort.com/natmusapi-proxy"
           ;"//testapi.natmus.dk"
           "/v1/search/?query=(sourceId:" id ")" :credentials false)))

(defn <natmus-images [id]
  (go (let [imgs (->> (get (<! (<natmus-id id)) "Results")
                      (map #(get % "relatedSubAssets"))
                      (filter #(< 0 (count %)))
                      (first)
                      (map #(<natmus-id (get % "sourceId")
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

(defn preload-img [oid img-url]
  (->  (js/document.createElement "img")   
      (aset "onload" #(dispatch [:360-img-load oid img-url]))
      (aset "src" img-url)))

(register-handler 
  :360-img-load
  (fn  [db  [_ oid url]]  
    (update-in db [:360 oid "loaded"] #(conj (or % #{}) url))))

(register-handler 
  :360-images 
  (fn  [db  [_ oid imgs]]  
    (doall (map preload-img oid imgs))
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

        [:img {:src (nth (:imgs o) pos)
               :on-mouse-move (partial handle-move pid)
               :width "100%"}]))))

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
(defn dom->sxml [dom]
  dom)

(defn xml->sxml [s]
  (let [parser (js/DOMParser.)
        dom (.parseFromString parser s "application/xml")]
  (dom->sxml dom)))

(defn <film-page [n]
  (go
    (let [xml (<! (<ajax 
                    (str
                      "http://nationalfilmografien.service.dfi.dk"
                      "/movie.svc/list?startrow=" n "00&rows=100")
                    :result :text
                    ))]
      (log (xml->sxml xml))
    [:div
     xml
     ])))
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
             "" [film-page-list])}))
    ))
