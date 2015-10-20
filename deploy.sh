#!/bin/bash -vx
killall java
lein clean 
npm install 
lein cljsbuild once dist 
cp resources/public/solsort.js ~/html/solsort.js 
cp package.json ~/html/package.json
cd ~/html/ 
npm install && npm start
echo "now if everything is ok:"
echo "git commit -am solsort.js ; git push ; cd ~/solsort"
