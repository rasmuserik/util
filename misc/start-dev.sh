npm install
./misc/gendoc.sh &
node ./misc/mubackend.js &
lein clean 
lein marg &
lein figwheel
