#!/bin/bash -vx
killall java
lein clean 
npm install 
lein cljsbuild once dist 
lein deploy clojars
#cd ~/html/ 
#git pull ;
#cp ~/solsort/resources/public/solsort.js ~/html/solsort.js 
#cp ~/solsort/package.json ~/html/package.json
#npm install && npm start
#git commit -am solsort.js ; git push ; cd ~/solsort
#curl jhjhjhjhjh.solsort.com/site-update
#cd ~/solsort/
#lein clean
