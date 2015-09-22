;; # Lemon
;;
;; This repository will contain widgets and apps for Tinkuy/NewCircleMovement/...
;;
;; It is currently in very initial development, and not really usable for anything yet.
;; More info will follow later.
;;
;; ## Development environment / Getting started
;;
;; Install leiningen, and:
;;
;; - `lein figwheel`
;;
;; Then a local development-version of the NewCircleMovement/tinkuy ruby app,
;; running on port 3000, will connect directly to the clojurescript environment,
;; with repl-support with the tinkuy-site.
;;
;; # Literate source code
;;
;; I like the concept of
;; [literate programming](https://en.wikipedia.org/wiki/Literate_programming),
;; where the code is written as a document to be read by humans too.
;; In the following there will be the actual code, intermixed with a description
;; of the ideas behind it.
;;
;; ## Namespace definition
;;
;; Define the module, and declare the dependencies. Use the standard ClojureScript modules
(ns solsort.apps.lemon
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])
  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]

    ;; It uses the re-frame framework.
    ;;
    ;; If you are interested in client-side development in general,
    ;; read the [re-frame](https://github.com/Day8/re-frame) readme.
    ;; As that is a very good document about how to structure application.

    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch dispatch-sync]]

    ;; And some of my own utility functions, that I share among projects.
    ;; Routing, platform-abstraction, utilities, etc.
    [solsort.util :refer [route log unique-id <p]]
    [solsort.net :refer [<ajax]]
    [solsort.ui :refer [app input default-shadow add-style icon]]
    ))

;; ## API-mock
;;

(defn extract-solsort-data []
  (loop [e  (js/document.getElementsByClassName "solsort-data") 
         i 0 
         acc {}]
    (if (<= (.-length e) i)
      acc
      (recur e (inc i) 
             (into acc (-> e
                           (aget i)
                           (.-dataset)
                           (js/JSON.stringify)
                           (js/JSON.parse)
                           (js->clj)))))))
(defn current-user-id []
  (go (get (extract-solsort-data) "userid")))
;; ## Sample/getting started code
;;
;; This is just a small hello-world app, will be replaced by the actual code soon.
(defn login-page []
  [:div {:style {:text-align :center
                 :display :inline-block
                 :position :absolute
                 :top 0
                 :height "100%"
                 :width "100%"
                 }}
   [:h1 [icon "emojione-lemon"] "LemonGold"]
   [:div {:style {
                  :display :inline-block
                  :box-shadow default-shadow
                  :padding-top 30
                  :width 300}}
    [:div [input :style {:width 240} :placeholder "username" :name "username"]]   
    [:div [input :style {:width 240} :placeholder "password" :type "password ":name "password"]]
    [:div [:button.float-right
           {:style {:margin 15 }
            :on-click #(js/alert "not implemented yet")} "login"]]
    ]]

  )

(defonce tinkuy-db (js/PouchDB. "tinkuy"))
(defn <upsert [db k f]
  (go (let [doc (<! (<p (.get db k)))
            doc (or (clj->js {:_id k}))
            doc (f doc)]
        (<p (.put db doc)))))
(register-sub :tinkuy-events (fn [db _] (reaction (:tinkuy-events @db))))
(register-handler 
  :tinkuy-events 
  (fn [db [_ events]] 
    (assoc db :tinkuy-events events)))
(go 
  (let [events  (<! (<p (.get tinkuy-db "events")))]
    (dispatch [:tinkuy-events (get (js->clj events)"all")])))
(defonce init
  (go 
    (let [events (<! (<ajax "https://www.tinkuy.dk/events.json"))]
      (when events
        (dispatch-sync [:tinkuy-events events])
        (<upsert tinkuy-db "events" (fn [o] (aset o "all" (clj->js events)) o))))))

(defn calendar []
  (let [events 
        (->> @(subscribe [:tinkuy-events]) 
             (filter #(% "confirmed"))
             (filter #(<= (-> (js/Date.) (.toISOString) (.slice 0 10)) (% "startdate")))
             (take 50)
             ; url starttime startdate id hour name duration minut confirmed description
             (map (fn [e] 
                    (let [date (.slice (e "startdate") 0 10)
                          starttime     (.slice (e "starttime") 11 16)
                          url (.slice (e "url") 0 -5)
                          title (e "name")
                          description (e "description")] 
                      [:div 
                       {:style {:white-space "nowrap"
                                :overflow "hidden"
                                :background "rgba(255,255,255,0.9)"
                                :box-shadow (str default-shadow)
                                :margin "1em"
                                :padding "0.5em" } 
                        :on-click (fn []  
                                    (js/open url)
                                    nil)
                        }
                       [:strong title] [:br] 
                       [:span 
                        (["Søndag" "Mandag" "Tirsdag" "Onsdag" "Torsdag" "Fredag" "Lørdag"]
                         (.getDay (js/Date. date)))  " "
                        date " " starttime] [:br] 
                       description]))))]

    [:div
     [:h1 {:style {:text-align :center}} "Events i Tinkuy"]
     (if (zero? (count events))
       [:center "Loading."]
       (into [:div] events))
     [:div {:style {:text-align "center" :padding "2em"}} "• • •"]]))

(defn show-log []
  [:div.container
   [:h3 "Debugging log:"]
   
   
    (into [:div]
      (map
     (fn [e] [:p {:key (unique-id)} (.slice (str e) 1 -1)])
     (reverse @(subscribe [:log]))))])
(defn about []
  [:div.container
   [:h1 "Om LemonGold"]
   [:p "Dette er en prototype, udviklet for New Circle Movement af RasmusErik / solsort.com"]
   [:p "Icons by " @(subscribe [:icon-authors])]
   ])
(def
  views
  {:lemon [show-log]
   :calendar [calendar]
   :profile [login-page]
   :items [login-page]
   :show-log [show-log]
   :about [about]} 
  )

(defn view []
  (get views @(subscribe [:view]) [calendar]))
(route
  "lemon"
  (fn []
    (app
      {:type :app
       ;:title "LemonGold"
       ;:navigate-back {:event ['home] :icon "emojione-lemon"}
       ;:actions [ {:event [:log "pressed hello"] :icon "hello"} ]
       ;:bar-color "rgba(00,50,50,0.8)"
       :views [ 
               ;{:event [:view :lemon] :icon "emojione-lemon"}
               {:event [:view :calendar] :icon "emojione-calendar"}
               {:event [:view :profile] :icon "emojione-bust-in-silhouette"}
               ;{:event [:view :items] :icon "emojione-package"}
               {:event [:view :show-log] :icon "emojione-clipboard"} 
               {:event [:view :about] :icon "emojione-question"}

               ]
       :html [view] })))

(add-style
  {".solsort-app-container"
   {:background "#fff7e0"}
   :.container 
   {:margin "0em 1em 0em 1em" }
   }
  )
