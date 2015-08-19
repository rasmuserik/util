(defproject lemon "0.0.1-SNAPSHOT"
  :description "Initial prototype of the tinkuy app"
  :url "https://github.com/rasmuserik/tinkuy"
  :license {:name "Public Domain" :url "https://creativecommons.org/publicdomain/zero/1.0/"}

  :dependencies
  [[org.clojure/clojure "1.7.0"]
   [org.clojure/clojurescript "1.7.107"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]
   [cljsjs/pouchdb "3.5.0-0"]
   [garden "1.2.5"]
   [facjure/mesh "0.3.0"]
   [re-frame "0.4.1"]
   [reagent "0.5.0"]]

  :plugins
  [[lein-cljsbuild "1.0.6"]
   [lein-figwheel "0.3.7"]
   [lein-kibit "0.1.2"]
   [lein-npm "0.6.1"]
   [lein-bikeshed "0.2.0"]
   [cider/cider-nrepl "0.9.1"] 
   [lein-shell "0.4.1"]]

  :npm 
  {:dependencies
   [[connect-fonts-ubuntu "0.0.1"]
    [normalize.css "^3.0.3"]]}

  :source-paths ["src"]

  :clean-targets ^{:protect false} 
  ["resources/public/js/compiled" 
   "target"
   "figwheel_server.log"
   "docs"
   "node_modules"
   ]

  :aliases {"gendoc" ["shell" "./gendoc.sh"]}

  :cljsbuild 
  {:builds 
   [{:id "dev"
     :source-paths ["src"]
     :figwheel { 
                :websocket-host ~(.getHostAddress (java.net.InetAddress/getLocalHost))
                :on-jsload "lemon.core/on-js-reload" }
     :compiler {:main lemon.core
                :asset-path "js/compiled/out"
                :output-to "resources/public/js/compiled/main.js"
                :output-dir "resources/public/js/compiled/out"
                :source-map-timestamp true }}

    {:id "dist"
     :source-paths ["src"]
     :compiler {:output-to "resources/public/js/compiled/main.js"
                :main lemon.core
                :optimizations :advanced
                :pretty-print false}}]
   ; TODO, notes:
   ; https://github.com/cemerick/clojurescript.test/tree/master/resources/cemerick/cljs/test
   ; https://github.com/emezeske/lein-cljsbuild/blob/master/doc/TESTING.md
   ; :test-commands
   ; {"unit-tests" ["phantomjs" :runner "resources/public/js/compiled/main.js"]}
   }

  :figwheel {:nrepl-port 7888})
