(ns solsort.apps.hack4dk-2015
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer  [go alt!]])
  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]

    [re-frame.core :as re-frame :refer [subscribe register-sub register-handler dispatch dispatch-sync]]

    [solsort.util :refer [route log unique-id <p]]
    [clojure.string :refer [replace]]
    [solsort.net :refer [<ajax]]
    [solsort.ui :refer [app input default-shadow add-style icon]]
    ))


(route
  "extract-data"
  (fn [o]
    (go 
      (js/console.log 
        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=(type:asset)" ))))
      {:type "text/plain"
       :content (str (<! (<ajax "http://testapi.natmus.dk/v1/Search/?query=(type:asset)")))
       })
    )
  )



(route
  "natmusapi-proxy"
  (fn [o]
    (go
      (let [url (replace 
                    (o "url")
                    #"^.?natmusapi-proxy"
                    "http://testapi.natmus.dk")
            result (<! (<ajax url :result :json)) ]
      (log o url)
      (js/console.log result)
      {:type :json
       :json result
       }))))
(register-handler 
  :360-images
  (fn  [db  [_ imgs]]  
    (assoc-in db [:360 imgs] 
              (map 
                (fn [src] {:src src
                           :state :init })
                imgs))))

(defn <natmus [q]
  (<ajax (str "http://localhost:4321/natmusapi-proxy"
              "/v1/search/?query=" q)))
(comment js/console.log 
        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=(categories:Rotationsbilleder)" )))
      (js/console.log 
        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=(sourceId:106977)" ))))
      (js/console.log 
        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=(sourceId:106677)" ))))
      (js/console.log 
        (clj->js (<! (<ajax  "http://testapi.natmus.dk/v1/Search/?query=solvogn* and (categories:Rotationsbilleder)" )))))

(route
  "hack4dk-360"
  (fn [o]
    (go
      (let [im-config (<! (<ajax (o "imgs")))]
        (<! (timeout 1000))
        (js/console.log (clj->js (<! (<natmus "(sourceId:106977)"))))
        {:type :html
         :html [:div 
                (comment map (fn [im] [:img {:src im
                                     :width "100%"

                                     }]) (im-config "imgs"))
                ]}))))
