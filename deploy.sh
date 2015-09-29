lein clean && npm install && lein cljsbuild once dist && cp resources/public/solsort.js ~/html/solsort.js && cd ~/html/ && git commit -am solsort.js && git push && cd ~/solsort
