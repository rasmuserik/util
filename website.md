This document is used for drafting the content for solsort.com.

Visit http://solsort.com/ for the canonical up to date version. The content below probably includes notes that are not applicable yet.


# Pages

## solsort.com

I write, debug, and optimise code,  <br>
&nbsp;  analyse data, and make HTML5-widgets, <br>
&nbsp;  collaborate with all levels of stakeholders.

Contact: RasmusErik Voel Jensen, +45 60703081, re@solsort.com







Selected professional experiences:

- HTML5 widgets, such as 360º-viewer, and graph visualisation of library data.
- Backends, such as combining datasources to live event strem, and mining usage-data yielding recommendation service.
- Problem solving for web agency, ie. performance+portability+mobile optimisation, implementing visual effects
- Bugfixing JavaScript engines Rhino(Java), SpiderMonkey(Firefox), - also implemented my own JavaScript dialect for low-end devices
- Creating/teaching university courses, and doing conference presentations
- Getting search engine into production, including automating acceptance tests from stakeholders, implementing query optimisations etc.
- Project management in digitisation project, hiring, making software for improving workflow, etc.
- Runtime x86-machine code analysis for partial evaluation. Performance optimisation of mathematical simulation.


## Product: 3-day sprint

I work on your project Monday-Wednesday or Friday-Sunday.

- Collaboration on-site / in person, with clarification of needs, knowledge sharing etc.
- Concrete delivery: the sprint ends out with delivery of concrete progress, and a written documentation of what has been done.
- Satisfaction guarantee: money-back if not satisfied with result (excluding cost of transport and similar expenses).

Next available sprint is: December 7.

Typical price for a 3-day sprint is DKK 19.200 (within Denmark) or €2880 (within EU).







Why 3-day sprints:

- Fixed-length unit of work makes scope of delivery more predictable 
  - See documentation of previous 3-day sprints, to get impression of expected delivery
- Focus 100% on the sprint, as interruptions etc. can be postponed a couple of days.
- Possible to include short planning/demo/retrospective without too much overhead.
- Quick iterations within larger project, means lower risk, better communication.
- Deadline enforces efficiency, both in planning, execution and delivery.
- Fits within business week, as well as part-time startups(weekend-projects) 
- Enough time to make concrete progress/delivery.

## Product: Hosting

When I make solutions that runs on top of the solsort platform, I also offer to take of operations (hosting / keeping it running etc). 

The price is DKK480/€64 monthly, or DKK4800/€640 yearly, which includes

- Running backend for the solsort platform
- A WordPress instance (if the solution needs content management).
- Regular updates, etc.





Everything is open source, so you can also host it yourself if you have a linux instance, and be responsible of keeping it up to date etc.


## The Solsort Platform

I am working on a solution that makes it easier to quickly create HTML5 apps and widgets.

It is currently under development, and I am using it for various prototypes.

Intended key features and technologies:

- Targeting mobile-first offline-first HTML5 apps
- Minimal backend, will provied authentication, storage-sync, communication, and hosting of semi-static resources
- Coded in primary in ClojureScript, - language support for functional programming, communicating sequential process, reactive programming, persitent data structures, etc. actually makes me more productive
- Content management in WordPress (just accessed via the API, makes it easy to integrate with other existing solutions, and many end-users will already knows it)

## RasmusErik

![RasmusErik](//solsort.com/icons/rasmuserik.jpg)

Independent computer scientist and software developer.

Tingskrivervej 21, 3tv <br>
2400 København NV <br>
+45 60703081 <br>
hej@solsort.com

[...]




## Posts

# 3-day sprints
## 2015-11-18 

## 2015-11-15 

The goal this sprint is to follow up on several practical things:

- Create/use new ssl certificate letsencrypt
- page-content on solsort.com
  - solsort.com: Software development
  - Product: 3-day sprint
  - Product: Hosting
  - Solsort platform
- regning
- automated backups

- evt. BibApp code
- evt. split into cdn/wp + API-server
- evt. wordpress-template
- evt. writings
  - mubackend: auth + `{public:{...},private:{...},incoming:[...],outgoing:{$RECEPIENT:[],...}` reactive database
  - database on top of ipfs
  - mobile language
  - virtues now and then
  - distributed ledger
  - portfolio posts
- evt. post-categories: 3-day sprints, Portfolio, IT, General


## 2015-11-11 BibApp Infrastructure

This sprint is about building building search service, and start building Bibliographic App.


Results:

- ElasticSearch service running with bibliographic data for materials with covers in brønd
- Figured out which books in the dataset that have cover on bogpriser.dk
- Merge `/bib` and `/bibdata` web services
- Make semantic marked-up html on `/bib` be based on the new data set / data model (this is the base for showing bibliographic data later on).

Productivity: Low - used a day on non-related computer science research (mainly programming language theory)

Tools:

- ElasticSearch, ClojureScript, Python

Starting point:

- Had base data from previous sprint, which just needed some cleanup / filtering by covers before put into ElasticSearch
- Already had the semantic markup code from the previous version of `/bibdata`, which just needed to run on the new DB

Takeways:

- More experience with ElasticSearch
- Thoughts on mobile language: Currying and typed lambda calculus leads to functions with closures as first class values, which can also be the building block of loops etc. Abstraction simplifies.


References:

- http://solsort.com/es/bib/ting/_search?q=murakami&pretty=true
- http://solsort.com/bibdata/ting/870970-basis:23645564

## 2015-11-06 solsort platform and practical stuff


The focus of this sprint was the infrastructure for solsort platform, plus some practical stuff. 

Results:

- Responsive grid, decision, and implementation. Decided on a 24 column grid, as it is friendly for the golden ratio (15/24) in addition to the usual 12-grid split. Implemented for mobile first with 3 responsive stops (mobile-portrait, mobile-landscape/tablet-portrait, tablet-landscape/desktop), using the same widths as android adaptive UI.
- Started implementing utility functions for mubackend api, ie. SHA256 etc. using WebCryptography API
- Refactor documentation
- Other practical tasks (accounting, tidying, etc.)

Productivity: Low - practical stuff took focus

Tools:

- ClojureScript, HTML5, CSS

Starting point:

- Previousely created a similar recommendation calculation
- Have taught eigenvector analysis etc., but never applied it to problems this large before

Takeways:

- Getting experience with WebCryptography API
- Need to allocate more time in calender for coding, in my own sprint where there are also other tasks.

References:

## 2015-10-28 uccorg

Something was wrong between frontend and the backend in the digital art project at UCC. As I had written the code for the backend, I was asked to step in and help solve it:

Results:

- Found and fixed bug with missing agents in frontend visualisation code
- Solved the obscure problem with the connections not working. Turned out that all the odroids had the same MAC-address, which really messed up the network.
- Various fixes in the frontend code

Productivity: Normal

Tools:

- JavaScript, CoffeeScript, odroids/android/ssh/arp/...

Starting point:

- I had made the backend code for the system, but have had nothing to do with the network setup, nor the front end, nor the odroids before

Takeaways:

- Network behave very strange when there it contains 30 devices with same MAC address, but if you push lot of packets through, it is sometimes possible to access the nearest device before TCP times out...

References:

- https://github.com/UCC-Organism/ucc-organism/compare/53fb1c50210fe0bccbff2dbd5477ffbd623276da...b48d16b6a15f03609d7036b9e41872ddd87c105e
- temporary: http://ssl.solsort.com:8080/ http://ssl.solsort.com/uccorg/

## 2015-10-20 Tinkuy.dk and ClojureScript Infrastructure

This sprint I focus on tinkuy.dk and my ClojureScript infrastructure

Results:

- tinkuy.dk
  - editable profile details
  - list of public profiles
  - merged fix of issue #69, #74, and #79 into master
  - deployed to tinkuy-staging for testing
  - fix date-dependent part of blackbox-testing, was issue #30
- started looking into better DB-abstraction for cljs-apps
  - Installed LevelDB in nwjs as storage backend
  - Designed API for new version of MuBackend, http://solsort.com/2015/10/20/mubackend-revisited/
  - Trying out watching sync'ed reactive in-memory clojurescript atom for access, as research for viability of autosync'ed database, looks good

Productivity: Normal

Tools:

- Ruby/Rails, CouchDB, ClojureScript, nw-gyp/nwjs, reagent

Starting point:


Takeways:

- CPouchDB does not really perform / feels unnatural
- nw-gyp for native modules in node-webkit

References:

- http://solsort.com/2015/10/20/mubackend-revisited/
- https://github.com/NewCircleMovement/tinkuy
- https://github.com/rasmuserik/solsort-util
- http://tinkuy-staging.heroku.com

## 2015-10-15 Library data

Preprocessed/analysed some library data, to build an app upon later on:

Results:

- Recommendations/related materials, based on similarity measure similar to cosine-similarity, but with slightly different weight for more relevant results.
- Eigenvector analysis (truncated SVD) of materials/loans, which can be used for a fast "relatedness" distance between materials (and will also be interesting to explore, to see if it can be used for a computational/empirical aproach to "genres")
- Easy to use data model (triples a la LOD) from different source, - and loaded into CouchDB
- Compressed local copy/backup of data

Productivity: Normal

Tools:

- python, leveldb, gensim, couchdb

Starting point:

- Previousely created a similar recommendation calculation
- Have taught eigenvector analysis etc., but never applied it to problems this large before

Takeways:

- It is easy to run SVD on large datasets. (unlike last time I looked at it, 10 years ago). Gensim crushes large data suprisingly fast (200 main eigenvalues on a ca. 1Mx500K sparse matrix in <16GB and less than a night).
- Mahout/spark is overkill+overhead when you do not need it nor have a cluster. (Installed Cloudera/CDH, spark etc. and got started, but then realised that it was easier just to implement it in python)

References:

- Sample result data: https://solsort.com/db/bib/870970-basis:26480388
- Code: http://github.com/rasmuserik/bib-data

## 2015-10-04 Hack4DK

Participated in the yearly danish culural heritage hackathon:

Results:

- An embeddable viewer for 360º rotational images from the collection of the National Museum, which makes it easier for people to share these rotational images.
- Semantic markup of data from Det Danske Filminstitut, which will make the data discoverable by search-engines, robots, etc.

Producitivity: Normal

Tools:

- ClojureScript, HTML5, re-frame/reagent/react

Starting point:

- The embeddable 360º rotational viewer was written from scratch, as I wanted to make it using/improving my primary toolbelt/tech-stack. I already knew the basics of optimising etc. 360º-rotational images, as I had worked on a similar project before.
- I have worked with semantic markup, with the danish library data. so I knew how to approach it, though I did not use any framework for generating it.

Takeaways:

- When making embeddable widget, a good idea is also to make an embed-code generator, so it is even easier for non-technical bloggers etc. to use it.
- Working with the XML-data from dfi, was super easy thanks to ClojureScript. This was a pleasant suprise, as I had not used ClojureScript for XML-processing before.
- If I continue publishing semantic data, I should make some tools to make it even easier, as I was kind-of redoing the same things, that I was doing with the library data.

References:

- Blogpost with examples: http://solsort.com/2015/10/02/3-day-sprint-hack4dk/
- Code: http://github.com/rasmuserik/solsort-util
