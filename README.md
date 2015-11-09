    
[![build status](https://travis-ci.org/rasmuserik/solsort-new.svg?branch=master)](https://travis-ci.org/rasmuserik/solsort-new)
[![cljs dependency status](http://jarkeeper.com/rasmuserik/solsort-new/status.png)](http://jarkeeper.com/rasmuserik/solsort-new)
[![node.js dependency status](https://david-dm.org/rasmuserik/solsort-new.svg)](https://david-dm.org/rasmuserik/solsort-new)

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

# Case study: Library app

## Plan

Indhold:

- search
  - (søge)historik (vægtet m. popularitet)
    - tidligere søgninger/visninger
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

