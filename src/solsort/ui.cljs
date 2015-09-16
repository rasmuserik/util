(ns solsort.ui
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [clojure.string :as string :refer  [split]]
    [re-frame.core :as re-frame :refer [register-sub subscribe register-handler dispatch dispatch-sync]]
    [solsort.misc :as misc :refer [function? chan? unique-id]]
    [solsort.net :as net]
    [solsort.style :refer [clj->css]]
    [reagent.core :as reagent :refer  []]))

(defonce initialise
  (do
    (js/React.initializeTouchEvents true)
    (js/window.addEventListener "resize" #(dispatch [:update-viewport]))))

(register-sub :view-dimensions
              (fn  [db _]  (reaction
                             [(get-in @db  [:viewport :width])
                              (get-in @db  [:viewport :height])])))
(register-sub :width  (fn  [db _]  (reaction  (get-in @db  [:viewport :width]))))
(register-sub :height  (fn  [db _]  (reaction  (get-in @db  [:viewport :height]))))
(register-sub :icons (fn  [db _]  (reaction  (:icons @db))))

(register-handler
  :update-viewport
  (fn [db _ _]
    (-> db (assoc-in [:viewport :width] js/window.innerWidth)
        (assoc-in [:viewport :height] js/window.innerHeight))))

;; # Components
(defn style [o]
  [:style {"dangerouslySetInnerHTML"
           #js {:__html (clj->css o)}}])
;; # App
;; ## Notes
;;
;; We try to follow [iOS Human Interface Guidelines](
;; https://developer.apple.com/library/ios/documentation/UserExperience/Conceptual/MobileHIG/),
;; but with a crossplatform focus. Secondary we accomodate
;; [Android Material Design](http://developer.android.com/design/) where possible.
;; The iOS guidelines are required get into the apple app-store.
;;
;; Common patterns are abstract, such that they can be implemented in a native way
;; on different platforms.
;;
;; We keep a database of creative commons icons, which are used within the app
;; (when no platform icon is available). https://thenounproject.com/ is good source for this,
;; though check that the icon follows the design guidelines, and include the license info
;; when loading it into the database.
;;
;; Data-notes
;; - `:type` - the type of the application
;;   - `:static` - static html or data, generated in parallel
;;   - `:html5` - standard html5 app
;;   - `:nwjs` - nwjs for daemons and desktop apps
;;   - `:cordova` - LATER additional apis available
;;   - `:extension` - LATER browser extension mozilla/chrom/opera, WebExtensions API
;;   - `:ios` - LATER react-native
;;   - `:android` - LATER react-native
;; - viewport
;;   - `:title` view title
;;   - `:navigate-back` back-button with `:event` and optional `:title`
;;   - `:actions` sequence view-specific actions with `:icon`, `:title`, `:active` and `:event`,
;;     similar to iOS Toolbar or Android Actions
;;   - `:views ` sequence of views with `:icon`, `:title`, `:active` and `:event`,
;;     similar to iOS Tabbar or Android Navigation
;;   - `:width` `:height` width and height of the viewport including bars
;;   - `:scrollX` `:scrollY` scroll position within the viewport
;;   - `:transition` NOT IMPLEMENTED
;;   - `:style` stylesheet as map of style maps
;;   - `:done` static application content is ready to be send, - defaults to true
;;

;; ## Actual app component
(def bar-height 48)
(def bar-shadow "0px 0.5px 1.5px rgba(0,0,0,.5)")
(def bar-color "rgba(250,240,230,0.9)")
;(def bar-color "#f7f7f7")
(defn icon [id]
  (let [url (get @(subscribe [:icons]) id)]
    (js/console.log "url" url)
    (when-not url
      (dispatch [:load-icon id]))
    (if url
      [:img.icon-img {:src url}]
      [:span "[" id "]"] 
      )
    )
  )
(def app-style ; ###
  (ratom/reaction
    {:h1 {:background "red"}
     :.solsort-app {:background :blue}
     :.float-right {:float :right}
     :.float-left {:float :left}

     :.bar
     {:width "100%"
      :text-align :center
      :display :inline-block
      :background bar-color
      :font-size (* .4 bar-height)
      :box-shadow bar-shadow
      :line-height bar-height
      :height bar-height
      :position :fixed  }

     :.icon-img {:width "2em" :height "2em" :vertical-align "middle" :margin ".25em"}
     :.topheight {}
     :.barheight {:height bar-height}
     :.botbar { :bottom 0 }}))
(defn app [o] ; ###
  (let [title (:title o)
        navigate-back (:navigate-back o)
        actions (:actions o)
        views (:views o)
        content (:html o) ]
    (aset js/document "title" title)
    [:div {:style {:position "absolute"
                   :width "100%" }}
     (style @app-style)

     [:div.topbar.bar
      [:span.middle title]
      (when navigate-back
        [:span.float-left
         {:on-click #(dispatch (:event navigate-back))
          :on-touch-start (fn []  (dispatch (:event navigate-back)) false)

          }
         [icon (:icon navigate-back)]
         " " (:title navigate-back)])

      (when actions
        (into
          [:span.float-right]
          (map
            (fn [a] [:span.barbutton
                     {:on-click #(dispatch-sync (:event a))
                      :on-touch-start (fn [] (dispatch-sync (:event a)) false)}
                     " " [icon (:icon a)] " "])
            actions)))]
     (when views
       (into
         [:div.botbar.bar]
         (map
           (fn [a] [:span.barbutton
                    {:on-click #(dispatch-sync (:event a))
                     :on-touch-start (fn [] (dispatch-sync (:event a)) false)}
                    " " [icon (:icon a)] " "])
           views)))
     [:div.barheight]
     content
     (when views [:div.barheight])]))

