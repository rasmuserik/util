(ns solsort.apps.hack4dk-2015
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])
  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]

    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch dispatch-sync]]

    [solsort.util :refer [route log unique-id <p]]
    [solsort.net :refer [<ajax]]
    [solsort.ui :refer [app input default-shadow add-style icon]]
    ))


(route
  "extract-data"
  (fn [o]
    (go 
      (log 'here)
    {:type "text/plain"
     :content "hello"
     })
    )
  )

(register-handler 
  :360-images
  (fn  [db  [_ imgs]]  
    (assoc-in db [:360 imgs] 
              (map 
              (fn [src] {:src src
                         :state :init })
              imgs))))

(route
  "hack4dk-360"
  (fn [o]
    (go
      (log o)
      (log (o "imgs"))
      (let [im-config (<! (<ajax (o "imgs")))]
        (log 'im-config im-config)
      {:type :html
       :html [:div 
              (map (fn [im] [:img {:src im
                                   :width "100%"
                                   
                                   }]) (im-config "imgs"))
              [:h1 "hi"]
              [:img {:src "blah"
                          :width "100%"
                          }]] }))))
