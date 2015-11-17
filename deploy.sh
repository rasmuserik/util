#!/bin/bash -vx
killall java
lein clean 
npm install 
lein cljsbuild once dist 
cp resources/public/solsort.js ~/html/solsort.js 
cp package.json ~/html/package.json
cd ~/html/ 
git pull ;
npm install && npm start
git commit -am solsort.js ; git push ; cd ~/solsort
curl jhjhjhjhjh.solsort.com/site-update
cd ~/solsort/
lein clean
