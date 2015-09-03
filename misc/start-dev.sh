npm install
./misc/gendoc.sh &
#node ./misc/mubackend.js &
npm start &
lein clean 
lein marg &
lein figwheel
