[![build status](https://travis-ci.org/rasmuserik/solsort-util.svg?branch=master)](https://travis-ci.org/rasmuserik/solsort-util)
[![cljs dependency status](http://jarkeeper.com/rasmuserik/solsort-util/status.png)](http://jarkeeper.com/rasmuserik/solsort-util)
[![node.js dependency status](https://david-dm.org/rasmuserik/solsort-util.svg)](https://david-dm.org/rasmuserik/solsort-util)
[![Clojars Project](http://clojars.org/solsort/util/latest-version.svg)](http://clojars.org/solsort/util)

The solsort platform, including various utility libraries and apps.

Approach: 

- build apps, and add features to the platform as needed.

# Getting started - Currently pre-public-release.

not really released yet, do not get started

## Build commands

- `lein npm install` installs dependencies
- `lein figwheel` starts development server on
  [http://localhost:3449](http://localhost:3449/) with nrepl on port 7888.
- `lein clean` removes artifacts etc
- `lein kibit` and `lein bikeshed -m 1000` runs various style tests
- `lein cljsbuild once dist` builds minified version
- `lein ancient` check if dependencies are up to date
- TODO `lein cljsbuild test` builds and run unit-tests


# Backlog

1. Library app case study

## TODO

- network/persistent state, very useful for datarepresentation for lemongold, and user-infor etc. in library app

## Later
- unit testing
- selenium tests
- cljsjs bundle: polycrypt and polyfill

# Case study: LemonGold

# Case study: BibApp
## Plan

Indhold:

- search
  - forside
  - (søge)historik (vægtet m. popularitet)
    - tidligere søgninger/visninger/biblioteker
  - søgeresultater
    - klyngede forsider, add more, evt. zoom
  - materialevisning
    - metainfo
    - evt. forside
    - relaterede materialer
    - nærmeste bibliotek med materialet hjemme
- patron
  - mine lån
  - mine bestillinger
  - log out / clear history on device
- libraries
  - find bibliotek
    - på kort + gps
  - dette bibliotek
    - åbningstider
    - lokation
- ask
  - spørg en bibliotekar

General:

- search content also as prerendered html with linked data
- data
  - recommendations
  - distance measure
  - evt search engine

- data cleanup:
  - DK5 separate field
  - en/da type

# Platform notes (Not fully implemented yet)

see mubackend notes on solsort.com blog

# Changelog

- 0.0.4 make apps separate from of solsort-util
- 0.0.3 release with bibapp, - last release deployed as monolitic https://solsort.com/solsort.js
- 0.0.2 deploy to clojars with updated dependencies
- 0.0.1 initial version
