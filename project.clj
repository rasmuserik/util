(defproject solsort/util "0.1.2-SNAPSHOT"
  :description "Solsort utility-functions"
  :url "https://github.com/rasmuserik/util"
  ;:license {:name "Several licenses" :url "https://github.com/rasmuserik/util"}

  :dependencies
  [[org.clojure/clojure "1.8.0-RC4"]
   [org.clojure/clojurescript "1.7.170"]
   [org.clojure/core.async "0.2.374"]
   [reagent "0.5.1"]
;   [com.cognitect/transit-cljs "0.8.220"]
;   [cljsjs/pouchdb "5.1.0-1"]
;   [re-frame "0.6.0"]
   
   ]

  :plugins
  [[lein-cljsbuild "1.1.1"]
   [lein-ancient "0.6.8"]
   [lein-figwheel "0.5.0-2"]
   [lein-kibit "0.1.2"]
   [lein-bikeshed "0.2.0"]
  ; [cider/cider-nrepl "0.10.0"]
   ]

  :source-paths ["src"]

  :clean-targets ^{:protect false} 
  ["resources/public/out" 
   "resources/public/solsort.js" 
   ".lein.failures"
   "figwheel_server.log"
   "docs"
   ]

  :cljsbuild 
  {:builds 
   [{:id "dev"
     :source-paths ["src"]
     :figwheel {:websocket-host ~(.getHostAddress (java.net.InetAddress/getLocalHost))
                ;:on-jsload "solsort.main/start" 
                }
     :compiler {:main solsort.main
                :asset-path "out"
                :output-to "resources/public/solsort.js"
                :output-dir "resources/public/out"
                :source-map-timestamp true }}

    {:id "debug"
     :source-paths ["src"]
     :compiler {:main solsort.main
                :source-map-timestamp true   
                :optimizations :simple
                :pretty-print true}}
    {:id "dist"
     :source-paths ["src"]
     :compiler {:output-to "resources/public/solsort.js"
                :main solsort.main
                :externs ["misc/externs.js"
                                 "misc/express.ext.js"
                                 "misc/cljsjs-pouchdb.ext.js"]
                :optimizations :advanced
                :pretty-print false}}]
   }

  :figwheel {:nrepl-port 7888})
