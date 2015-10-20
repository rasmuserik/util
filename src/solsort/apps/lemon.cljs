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
;; # Namespace definition
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
    [solsort.misc :refer [next-tick]]
    [solsort.net :refer [<ajax]]
    [solsort.db :refer [db-url]]
    [solsort.ui :refer [app input default-shadow add-style icon]]
    ))

;; # DB
(defn db-init []
  (defonce tinkuy-users
    (do
      (let [tinkuy-users (js/PouchDB. "tinkuy-users" #js {:auto_compaction true})]
        (.sync tinkuy-users (db-url "tinkuy-users") #js {:live true})
        tinkuy-users))) 
  (defonce tinkuy-db 
    (do
      (let [tinkuy-db (js/PouchDB. "tinkuy" #js {:auto_compaction true})]
        ;(.sync tinkuy-db (db-url "tinkuy") #js {:live true})
        tinkuy-db))))

;; # API-mock
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
;; # Sample/getting started code
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

(defn <upsert [db k f]
  (go (let [doc (<! (<p (.get db k)))
            doc (or doc (clj->js {:_id k}))
            doc (f doc)]
        (<p (.put db doc)))))

(register-sub :tinkuy-events (fn [db _] (reaction (:tinkuy-events @db))))
(register-handler 
  :tinkuy-events 
  (fn [db [_ events]] 
    (assoc db :tinkuy-events events)))
(go 
  (db-init)
  (let [events  (<! (<p (.get tinkuy-db "events")))]
    (dispatch [:tinkuy-events (get (js->clj events)"all")])))

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
    (db-init)
    (defonce init
      (go 
        (let [events (<! (<ajax "https://www.tinkuy.dk/events.json"))]
          (when events
            (dispatch-sync [:tinkuy-events events])
            (<upsert tinkuy-db "events" (fn [o] (aset o "all" (clj->js events)) o))))))
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
   })

;; # Tinkuy widgets
(route 
  "tinkuy/behandler-list"
  (fn [o]
  (go
    (let [profiles 
          (->>  (-> tinkuy-users 
            (.allDocs (clj->js {:include_docs true})) 
            (<p) (<!) (js->clj) 
            (get "rows"))
             (map #(get % "doc"))
             (filter #(get % "profile-is-public")))]
    {:type :html
     :html 
     [:div
      (into 
        [:div]
        (map
          (fn [o]
          [:div {:style {:display :inline-block
                         :vertical-align :top
                         :margin 5
                         :width 150 :height 150
                         :overflow :hidden
                         }}
           #_[:div [:img {:alt "billeder kommer senere"
                  :width 150
                  :height 150
                  }]]
           [:div (o "firstname") " " (o "lastname")]
           [(if (o "profile-therapist") :strong :span) (o "profile-title")]
           [:div [:a {:href (o "profile-url")} (o "profile-url")]]
           [:div (map (fn [s] [:div s]) (.split (str (o "profile-description")) "\n"))]
           ])
          (sort-by #(str (if (% "profile-therapist") "a" "b") (.toUpperCase (% "firstname"))) profiles)))
      [:hr]]}))))

(defonce usersynced (atom #{}))
(defonce user-sync 
  (fn [o id]
    (fn [k]
      (when-not (@usersynced [id k])
        (swap! usersynced conj [id k])
        (dispatch-sync [:form-value k (o k)])
        (ratom/run! 
          (let [v @(subscribe [:form-value k])]
            (go
              (let [doc (<! (<p (.get tinkuy-users id)))
                    doc  (or doc  (clj->js  {:_id k}))]
                (when (not= (aget doc k) v)
                  (aset doc k v)
                  (.put tinkuy-users doc))
                ))))))))

(defn sync-name [userid]
  (go
    (let [firstname-elem (js/document.getElementById "user_firstname") 
          lastname-elem  (js/document.getElementById "user_surname") 
          firstname (.-value firstname-elem)
          lastname (.-value lastname-elem)]
      (<! (<upsert tinkuy-users userid (fn [o] 
                                         (aset o "firstname" firstname)
                                         (aset o "lastname" lastname)
                                         o))))))
(route 
  "tinkuy/user-edit"
  (fn [o]
    (go 
      (db-init)
      (assert (re-matches #"tinkuy:[0-9]*" (o "userid")))
      
        
      (let [userid (o "userid")
            obj (or (<! (<p (.get tinkuy-users userid))) #js {})
            obj (js->clj obj)]
        (try
          (let [firstname-elem (js/document.getElementById "user_firstname") 
                lastname-elem  (js/document.getElementById "user_surname") 
                firstname (.-value firstname-elem)
                lastname (.-value lastname-elem)]

            (when (or (not= firstname (o "firstname")) 
                      (not= lastname (o "lastname")))
              (<! (sync-name userid)))
            (aset firstname-elem "onchange" #(sync-name userid))
            (aset lastname-elem "onchange" #(sync-name userid)))
          (catch js/Object e (log 'error e)))
        (doall (map
                 (user-sync obj userid)
                  ["profile-therapist"
                  "profile-is-public"
                  "profile-title"
                  "profile-description"
                  "profile-url"]))
        {:type :html
         :html 
         [:div
          [:p 
           [input :name "profile-therapist" :type "checkbox"] 
           [:label {:for "profile-therapist"}
            " \u00a0 er behandler"]]
          [:p 
           [input :name "profile-is-public" :type "checkbox"] 
           [:label {:for "profile-is-public-input"}
            " \u00a0 offentlig profil, - kan ses af alle på hjemmesiden"]]
          #_[:p 
             [:label {:for "profile-image-input"}
              [:img {:alt "billede"}]] 
             [input 
              :name "profile-image" :type "file"]]
          [:p [:label {:for "profile-title-input"} "Titel/type: "] [:br] 
           [input 
            :name "profile-title" 
            :type "text" 
            :style {:width "100%"}]]
          [:p [:label {:for "profile-description-input"} "Beskrivelse:"] [:br] 
           [input 
            :name "profile-description"
            :type "textarea" 
            :rows 8
            :style {:width "100%"}]]
          [:p [:label {:for "profile-url-input"} "Url (hjemmeside eller anden kontaktinfo):"] [:br] 
           [input 
            :name "profile-url" 
            :type "text" 
            :style {:width "100%"}]]

          ]}))))
