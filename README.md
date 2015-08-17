[![Build Status](https://travis-ci.org/rasmuserik/lemon.svg?branch=master)](https://travis-ci.org/rasmuserik/lemon)
[![Dependencies Status](http://jarkeeper.com/rasmuserik/lemon/status.png)](http://jarkeeper.com/rasmuserik/lemon)

# Lemon - prototype of the tinkuy app

Initial prototype of tinkuy app, more notes/documentation to come here real soon.

# Build commends

- `lein npm install` install npm dependencies, specificly downloads normalize.css
- `lein figwheel` starts development server on [http://localhost:3449](http://localhost:3449/) with nrepl on port 7888.
- `lein clean` removes artifacts etc
- `lein marg` creates html documentation in `docs/`
- `lein kibit` and `lein bikeshed -m 1000` runs various style tests
- `lein cljsbuild once dist` builds minified version
- `lein cljsbuild test` builds and run unit-tests

# Random ideas

- selemium tests
