(ns solsort.apps.install-sites
  (:require-macros
    [cljs.core.async.macros :refer  [go]])

  (:require
    [solsort.misc :refer [log unique-id <exec <n <seq<!]]
    [solsort.router :refer [route all-routes]]
    [cljs.reader :refer [read-string]]
    [clojure.string :refer [join]]
    [cljs.core.async :refer [<!]]
    ))

(when (and (some? js/window.require)
           (some? (js/window.require "fs"))))
(defonce has-error (atom false))
(defonce cfg (atom nil))
(defonce fs (when (some? js/window.require) (js/require "fs")))
(defonce dl-path "/tmp/dl/")
(defonce base-path "/tmp/new-solsort/")
(defn local-name [site] (str (re-find #"[^.]+" site) ".localhost"))
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
          ext (first (re-find #"(tar.gz|[^./]*)$" url))]
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
      "tar.gz" (<e "install -d " dst ";"
                "cd " dst ";"
                "tar xzf " src)
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
            id site
            site ((:sites @cfg) site)
            hostname (.hostname (js/require "os"))
            dev (contains? #{"panther" "test" "ubuntu" "monolith"} hostname)
            protocol (or (:protocol site) "http://")
            site-name (if dev (local-name id) id)
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
              "define('DB_PASSWORD', '" (:db-password site) "');\n"
              "define('WP_DEBUG', '" dev "');\n"
              "define('WP_HOME', '" protocol site-name "');\n"
              "define('WP_SITEURL', '" protocol site-name "');\n"
              
              ))))))

(defn nginx-config [site] ; ## 
  (let [id (first site)
        sites (:sites @cfg)
        site (second site)
        hosts (join " " (conj (get site :hosts []) id (local-name id)))]
    ["server"
     [:server_name hosts]
     [:root (str "/solsort/sites/" id "/wordpress")]
     [:index "index.html index.php"]
     [:listen 80]
     [:listen 443 "ssl"]
     [:ssl_certificate "/solsort/ssl/blog_solsort_com.crt"]
     [:ssl_certificate_key "/solsort/ssl/blog_solsort_com.key"]
     ["location /"
      [:try_files "$uri" "$uri/" "/index.php?args"]]
     ["location ~ \\.php"
      [:include "fastcgi_params"]
      [:fastcgi_param "SCRIPT_FILENAME" "$document_root$fastcgi_script_name"]
      [:fastcgi_pass "unix:/var/run/php5-fpm.sock"]]
     ["location /socket.io"
      [:proxy_http_version "1.1"]
      [:proxy_set_header "Upgrade" "$http_upgrade"]
      [:proxy_set_header "X-Forwarded-For" "$proxy_add_x_forwarded_for"]
      [:proxy_set_header "Connection" "\"upgrade\""]
      [:proxy_pass  "http://127.0.0.1:1234"]
      [:try_files "$uri" "$uri/" "@server"] 
      [:access_log "off"]]
     ["location /posts" 
      [:include "fastcgi_params"]
      [:fastcgi_param "SCRIPT_FILENAME" "$document_root/index.php"]
      [:fastcgi_pass "unix:/var/run/php5-fpm.sock"] 
      ]
     ["location /wp-json " [:try_files "$uri" "$uri/" "/index.php?$args"]]
     ["location ~ ^/20.*" [:try_files "$uri" "$uri/" "/index.php?$args"]]
     [(str "location ~ ^/(db|" (join "|" (all-routes)) ")") 
      [:try_files "$uri" "$uri/" "@server"]]
     ["location @server"
      [:proxy_set_header  "x-solsort-remote-addr"  "$remote_addr"]
      [:proxy_set_header  "x-solsort-site" id] 
      [:proxy_pass  "http://127.0.0.1:1234"] ]

     ]))
(defn nginx-to-str 
  ([o] 
   (nginx-to-str o "  "))
  ([o indent] 
   (if-not (vector? o)
     (if (keyword? o) (name o) (str o))
     (str
       "\n" indent
       (join 
         " " 
         (if (vector? (last o)) 
           (concat [(first o) "{"] 
                   (map #(nginx-to-str %  (str "  " indent)) (rest o)))
           (map #(nginx-to-str %  "") o)))
       (if (vector? (last o)) (str "\n" indent "}") ";")))))

(defn <nginx-config []
  (go (let [ config-template 
            (.readFileSync 
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
            (apply str 
                   (map #(nginx-to-str (nginx-config %)) (:sites @cfg))))))))


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
        (<! (<mysql-dbs site))
        (<! (<e "grep " (local-name site) " /etc/hosts ||"
                "sudo sh -c 'echo 127.0.0.1 " (local-name site) " >> /etc/hosts'"
                ))
        
        )))

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
    (<! (<copy "/home/rasmuserik/install/skeleton/solsort/ssl" base-path))
    (<! (<exec-install (:root @cfg) "/tmp/new-solsort/"))
    (<! (<seq<! (map <install-site (keys (:sites @cfg)))))
    (<! (<nginx-config))
    
    ))

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
      (<! (<e "sudo mv /tmp/new-solsort /solsort"))
      (<! (<e "sudo mv /solsort/nginx.conf /etc/nginx/nginx.conf"))
      (<! (<e "sudo /etc/init.d/nginx restart"))
      (<! (<e "sudo /etc/init.d/php5-fpm restart"))
      ) 
    (reset! cfg nil)))

(route "install-sites" ; ##
       (fn [o] 
         (when (nil? @cfg)
           (reset! cfg true)
           (go
             (<! (<install-sites))
             (reset! cfg nil)))
         {:type :html :html [:h1 "install"]}))

