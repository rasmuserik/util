(ns solsort.apps.install-sites
  (:require-macros
    [cljs.core.async.macros :refer  [go alt!]])

  (:require
    [cljs.test :refer-macros  [deftest testing is run-tests]]
    [reagent.ratom :refer-macros [reaction]]
    [reagent.core :as reagent]
    [solsort.misc :refer [log unique-id <exec <n <seq<!]]
    [solsort.router :refer [route-exists? route]]
    [cljs.reader :refer [read-string]]
    [clojure.string :as string]
    [solsort.style :refer [default-style-str]]
    [cljs.core.async :refer [>! <! chan put! take! timeout close!]]
    [re-frame.core :refer [register-handler register-sub subscribe]]))

(defn log-elem  []
  [:div  (map  (fn  [e]  [:div  {:key  (unique-id)}  (.slice  (str e) 1 -1)])
              (reverse @(subscribe  [:log])))])

(when (and (some? js/window.require)
           (some? (js/window.require "fs"))))
(defonce has-error (atom false))
(defonce cfg (atom nil))
(defonce fs (js/require "fs"))
(defonce dl-path "/tmp/dl/")
(defonce base-path "/tmp/new-solsort/")
(defn <e [& args] ; ##
  (go (let [s (apply str args)
            result (<! (<exec s))]
        (log '> s)
        (when (nil? result)
          (reset! has-error true)
          (log 'WARNING-FAILED s))
        result)))

(defn <load-source [[k url]] ; ##
  (go
    (let [fname (re-find #"[^/]*$" url)
          ext (re-find #"[^./]*$" url)]
      (<! (<e (str "install -d " dl-path ";"
                   "cd " dl-path ";"
                   (if (= "git" ext)
                     (str "git clone " url " " fname)
                     (str "wget -nc " url)))))
      [k [fname ext]])))

(defn <copy [src dst] ; ##
  (let [path (re-find #".*/" dst)]
    (go (<! (<e "install -d " path))
        (<! (<e "cp -a " src " " dst)))))

(defn <install-content [src dst] ; ##
  (let [[fname ext]  (-> @cfg (:sources) (src))
        src (str dl-path fname)]
    (case ext
      "zip" (<e "install -d " dst ";"
                "cd " dst ";"
                "unzip -x -o " src)
      "gz" (<e "zcat " src " > " dst)
      "git" (<e "install -d " dst ";"
                "rsync -a " src "/ " dst "")
      (<copy src dst))))

(defn <exec-install [o path] ; ##
  (go
    (<! (<e "install -d " path))
    (<! (<seq<!
          (map (fn [[src dest]]
                 (<install-content src (str path dest)))
               (seq (get o :content [])))))
    (<! (<seq<!
          (map
            #(<e "install -d " path % ";"
                 "chmod 777 " path % ";")
            (get o :write-dir []))))

    (<! (<seq<!
          (map
            (fn [[src dst]]
              (<e "rm -rf " path dst ";"
                  "ln -sf " src " " path dst))
            (get o :ln []))))
    (<! (<seq<!
          (map
            (fn [dir]
              (<e 
                "install -d /solsort/" dir ";"
                "rsync -a /solsort/" dir "/ " path dir))
            (get o :preserve []))))))

(defn <wp-config [site] ; ##
  (go (let [site-path (str base-path "sites/" site)
            site ((:sites @cfg) site)
            config-template (.readFileSync 
                              fs
                              "/home/rasmuserik/install/templates/wp-config.php"
                              "utf8" )]
        (when (= -1 (.indexOf config-template "/*!SOLSORT_CONFIG*/"))
          (log "WARNING: no /*!SOLSORT_CONFIG*/ in template")
          (reset! has-error true))
        (.writeFile
          fs (str  site-path "/wordpress/wp-config.php")
          (.replace
            config-template "/*!SOLSORT_CONFIG*/"
            (str
              "define('DB_NAME', '" (:db site) "');\n"
              "define('DB_USER', '" (:db-user site) "');\n"
              "define('DB_PASSWORD', '" (:db-password site) "');\n"))))))

(defn nginx-config [site] ; ## 
  ["server"
   [:server_name site]
   [:root (str "/solsort/sites/" site)]
   [:listen 80]
   [:listen 443 "ssl"]
   [:ssl_certificate "/solsort/ssl/blog_solsort_com.crt"]
   [:ssl_certificate_key "/solsort/ssl/blog_solsort_com.key"]
   ["location /"
    [:try_files "$uri" "$uri/" "index.php?$args"]]
   ["location /socket.io"
    [:proxy_http_version "1.1"]
    [:proxy_set_header "Upgrade" "$http_upgrade"]
    [:proxy_set_header "X-Forwarded-For" "$proxy_add_x_forwarded_for"]
    [:proxy_set_header "Connection" "\"upgrade\""]
    [:proxy_pass  "http://127.0.0.1:1234"]
    [:access_log "off"]]])
(defn nginx-to-str 
  ([o] 
   (nginx-to-str o "  "))
  ([o indent] 
   (js/console.log (str o indent))
   (log (if-not (vector? o)
          (if (keyword? o) (name o) (str o))
          (str
            "\n" indent
            (string/join 
              " " 
              (if (vector? (last o)) 
                (concat [(first o) "{"] 
                        (map #(nginx-to-str %  (str "  " indent)) (rest o)))
                (map #(nginx-to-str %  "") o)))
            (if (vector? (last o)) (str "\n" indent "}") ";"))))))

(defn <nginx-config []
  (log 'HJERE)
  (go (let [ config-template (.readFileSync 
                               fs
                               "/home/rasmuserik/install/templates/nginx.conf"
                               "utf8" )]
        (when (= -1 (.indexOf config-template "#SERVER_CONFIG#"))
          (log "WARNING: no #SERVER_CONFIG# in nginx template")
          (reset! has-error true))
        (.writeFile
          fs (str  base-path "nginx.conf")
          (.replace
            config-template "#SERVER_CONFIG#"
            (nginx-to-str (nginx-config "blah")))))))


(defn <mysql-dbs [site] ; ##
  (go (let [site ((:sites @cfg) site)
            db (:db site)
            user (:db-user site)
            password (:db-password site)
            db-pw (:db-root-password @cfg)]
        (log 'mysql-init db user password)
        (when (and db user password db-pw)
          (<e "echo \""
              "CREATE USER " user "@localhost IDENTIFIED BY '" password "';"
              "FLUSH PRIVILEGES;" 
              "\"| mysql mysql -u root --password=" db-pw ";"
              "echo \""
              "CREATE DATABASE " db ";"
              "GRANT ALL PRIVILEGES ON " db ".* to " user "@localhost;"
              "FLUSH PRIVILEGES;" 
              "\"| mysql mysql -u root --password=" db-pw ";"
              "true"
              )))))

(defn <install-site [site] ; ##
  (go (let [site-path (str base-path "sites/" site "/")]
        (<! (<e "install -d " site-path))
        (log 'default-content-for site)
        (<! (<exec-install (:default-site @cfg) site-path))
        (log 'custom-content-for site)
        (<! (<exec-install ((:sites @cfg) site) site-path))
        (<! (<wp-config site))
        (<! (<mysql-dbs site)))))

(defn <download-resources [] ; ##
  (go
    ;(<! (<e "rm -rf " dl-path))
    (let [config (-> fs
                     (.readFileSync
                       "/home/rasmuserik/install/config.clj")
                     (js/String)
                     (read-string))
          config (assoc
                   config :sources
                   (into {}
                         (<! (<seq<! (map <load-source (:sources config))))))]
      (reset! cfg config))))
(defn <create-sites [] ; ##
  (go
    (log 'start-install)
    (<! (<e "rm -rf " base-path))
    (<! (<exec-install (:root @cfg) "/tmp/new-solsort/"))
    (<! (<seq<! (map <install-site (keys (:sites @cfg)))))
    (<! (<nginx-config))))

(defn <install-sites  [] ; ##
  (go
    (<! (<e "cd /home/rasmuserik/install; git pull"))
    (<! (<download-resources))
    (reset! has-error false)
    (<! (<create-sites))
    (when (not @has-error)

      (<! (<e "(crontab -l ; echo @reboot /solsort/html/start-server.sh)"
              " | sort | uniq | crontab -"))
      (<! (<e "sudo rm -rf /solsort-old" ))
      (<! (<e "sudo mv /solsort /solsort-old" ))
      (<! (<e "sudo mv /tmp/new-solsort /solsort")))
    (reset! cfg nil)))

(route "install-sites" ; ##
       (fn [o] 
         (when (nil? @cfg)
           (reset! cfg true)
           (go
             (<! (<install-sites))
             (reset! cfg nil)))
         {:type :html :html (log-elem)}))

