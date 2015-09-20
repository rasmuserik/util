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

    [re-frame.core :as re-frame :refer [subscribe]]

    ;; And some of my own utility functions, that I share among projects.
    ;; Routing, platform-abstraction, utilities, etc.
    [solsort.util :refer [route log unique-id]]
    [solsort.ui :refer [app input default-shadow]]
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
(go
  (log 'uid (<! (current-user-id))))
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
   [:h1 "LemonGold"]
   [:div {:style {
                  :display :inline-block
                  :box-shadow default-shadow
                  :padding-top 30
                  :width 300}}
   [:div [input :style {:width 240} :placeholder "username" :name "username"]]   
   [:div [input :style {:width 240} :placeholder "password" :type "password ":name "password"]]
   [:div [:button.float-right
          {:style {:margin 15
                   :background "#fff"
                   :box-shadow default-shadow
                   :border :none
                   :padding 5
                   }
           :on-click #(js/alert "not implemented yet")} "login"]]
   ]]
  
  )

(defn show-log []
       [:div
        [:h3 "Debugging log:"]
        (map
          (fn [e] [:div {:key (unique-id)} (.slice (str e) 1 -1)])
          (reverse @(subscribe [:log]))) ]    )
(def
  views
  {:lemon [show-log]
  :calendar [show-log]
  :profile [login-page]
  :items [login-page]
  :show-log [show-log]}
  )

(defn view []
  (get views @(subscribe [:view]) [login-page])
  )
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
               {:event [:view :lemon] :icon "emojione-lemon"}
               {:event [:view :calendar] :icon "emojione-calendar"}
               {:event [:view :profile] :icon "emojione-bust-in-silhouette"}
               {:event [:view :items] :icon "emojione-package"}
               {:event [:view :show-log] :icon "emojione-clipboard"} ]
       :html [view] })))
