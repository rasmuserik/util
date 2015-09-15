;; # solsort
;;
;; More notes/documentation to come here real soon.
;;
;; ## Backlog
;;
;; ## Build commands
;;
;; - `lein npm install` installs dependencies
;; - `lein figwheel` starts development server on
;;   [http://localhost:3449](http://localhost:3449/) with nrepl on port 7888.
;; - `lein clean` removes artifacts etc
;; - `lein kibit` and `lein bikeshed -m 1000` runs various style tests
;; - `lein cljsbuild once dist` builds minified version
;; - `lein gendoc` regenerate project README.md from literate source code
;; - `lein ancient` check if dependencies are up to date
;; - TODO `lein cljsbuild test` builds and run unit-tests
;;
;;
;; ## Random ideas
;;
;; - selemium tests
;;
;; # Main code
;;
;; This just loads all components and apps, and dispatches the route based on the app
;;
(ns ^:figwheel-always solsort.main
  (:require
    [reagent.core :as reagent]
    [solsort.apps.bib]
    [solsort.apps.experiments]
    [solsort.apps.index]
    [solsort.apps.icon]
    [solsort.apps.lemon]
    [solsort.apps.rasmuserik]
    [solsort.util]
    ))
