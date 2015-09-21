(ns solsort.lib.app
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go go-loop alt!]])

  (:require
    [cljs.core.async.impl.channels :refer  [ManyToManyChannel]]
    [cljs.core.async :refer  [>! <! chan put! take! timeout close! pipe]]
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [clojure.string :as string :refer  [split]]
    [re-frame.core :as re-frame :refer [register-sub subscribe register-handler dispatch dispatch-sync]]
    [solsort.misc :as misc :refer [function? chan? unique-id unatom log]]
    [solsort.net :as net]
    [solsort.lib.icon :refer [icon]]
    [solsort.style :refer [clj->css style]]
    [reagent.core :as reagent :refer  []]))

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
(register-sub :show-bars (fn [db _] 
                           (reaction (< 0 (get @db :show-bars 0)))))
(register-handler :show-bars (fn [db [_ p]] 
                               (assoc db :show-bars ((if p inc dec) (get db :show-bars 0)))))

;; ## Actual app component
(def bar-height 48)
(def bar-shadow "0px 0.5px 1.5px rgba(0,0,0,.5)")
(defonce app-style ; ###
  (ratom/reaction
    {:.float-right {:float :right}
     :.float-left {:float :left}

     :.bar
     {:width "100%"
      :text-align :center
      :display :inline-block
      :font-size (* .4 bar-height)
      :box-shadow bar-shadow
      :line-height bar-height
      :height bar-height
      :margin 0
      :padding 0
      :position :fixed
      :z-index "2000"
      "-webkit-transform" "translateZ(0)"
      }

     :.topheight {}
     :.barheight {:height bar-height}
     :.botbar { 
               :transition "bottom 1s ease-in"
               :bottom (- bar-height)
               }}))


(defonce _ (do (js/setTimeout #(dispatch [:show-bars true]) 500)
              (js/setTimeout #(swap! app-style assoc-in [:.botbar :transition]  "") 2000)))
(defn app-html [o] ; ###
  (let [title (unatom (:title o))
        navigate-back (:navigate-back o)
        actions (:actions o)
        views (:views o)
        content (:html o) 
        bar-color (or (:bar-color o) "rgba(250,240,230,0.9)")
        show-top-bar  (or navigate-back actions (:show-title o)) 
        ]
    (aset js/document "title" title)
    (swap! app-style assoc-in [:.bar :background] bar-color)

    (swap!  app-style assoc-in [:.botbar :bottom] 
           (if  @(subscribe [:show-bars]) 0 (- bar-height)))
    [:div {:style {:position "absolute"
                   :width "100%" 
                   :min-height "100%"
                   :overflow "hidden"
                   }}
     (style @app-style)

     (when show-top-bar
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
              actions)))])
     (when views
       (into
         [:div.botbar.bar]
         (map
           (fn [a] [:span.barbutton
                    {:on-click #(dispatch-sync (:event a))
                     :on-touch-start (fn [] (dispatch-sync (:event a)) false)}
                    " \u00a0" [icon (:icon a)] "\u00a0 "])
           views)))
     (when show-top-bar [:div.barheight])
     content
     (when views [:div.barheight])]))

(defn app 
  ([o] {:type :html, :title (:title o) , :html [app-html o] })
  ([o1 o2] (app (into o2 o1))))
