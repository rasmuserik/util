This document keeps track of the 3-day development sprints at solsort.com, across different projects. The content will also be posted on the solsort blog.

# 2015 uccorg

- mismatch mellem skærme der ikke sker noget på
  - deploy
- researches etc.
- odroids views locally

Observations:

- Inconsistent view in frontend, (same frontend running parallel, - one has the agents from the backend, and the other doesn't)
- Researchers consistently missing in frontend
- Apparently misscoped variables in frontend: `attribAlias`, `targetNode`, `anotherAgent`, and possibly `P`, `lines`, `k`, `v`, and `e`. Might lead to bug/instability
- Repeatable missing agents, after switching view back and forth 

Results:

- Debug networking / find bug with odroids having same ip address
- Make frontend work/build on case-sensitive file systems
- Example screens are the same as the odroids
- Bugfix: researchers
- see git log
- set up api-proxy for testing
- build+hosted version of frontend with fixes for testing


- Status
- årsagen til at forskerne manglede da jeg skulle demo'e det, var at vi 


The 

- New agents might arrive, during the day. 


I have been looking missing agents in the frontend and it turns out:

- Agents without a study programme does not spawn, (and not every agent has a study programme): https://github.com/UCC-Organism/ucc-organism/blob/097936e8bda78b876cdb37cfae6922e652492135/src/ucc/sys/agentSpawnSys.js#L88
- Agents that are not part of the initial state, but arrives with events later, are silently ignored, instead of added: https://github.com/UCC-Organism/ucc-organism/blob/097936e8bda78b876cdb37cfae6922e652492135/src/ucc/data/Client.js#L107
- Something is odd about the data model, ie. the type/kind of agent seems to be expected to be stored in `programme`, which in the data represent the study program. https://github.com/UCC-Organism/ucc-organism/097936e8bda78b876cdb37cfae6922e652492135/master/src/ucc/sys/agentSpawnSys.js#L25
- Programme does not get assigned. https://github.com/UCC-Organism/ucc-organism/blob/097936e8bda78b876cdb37cfae6922e652492135/src/ucc/data/Client.js#L78

I did some quick hacks/workarounds which make the researcher agents etc. appear in frontend, but it does not process the type of agent etc., so needs to be worked through, and it would probably be good to take a general look at the data model.


I also spotted:

- Agents disappear when switching example screens, - indicates some instability in the data model, that could lead to other errors on the actual clients
- Many leaks to global scope (missing `var`), which may lead to unexpected bugs, - I'd recommend running some tools to identify scope leaks, and fixing those. ie. https://github.com/UCC-Organism/ucc-organism/blob/097936e8bda78b876cdb37cfae6922e652492135/src/ucc/sys/agentFlockingSys.js#L69 https://github.com/UCC-Organism/ucc-organism/blob/097936e8bda78b876cdb37cfae6922e652492135/src/ucc/stores/MapStore.js#L142 ... 
- Toilets probably does not work, as toilet location name is different in source code and the mail I got from you.
- Issues running frontend: it only build/bundled on case-insensitive file systems (fixed, so that works now), and `npm run watch` fails.


----

Sporadic connectivity to odroids. Both the faye-connection, and the requests for `client-config` (which should happen every 30s) happens rarely and at random, except from 10.251.25.32 and 10.251.25.46. To see if it was a network issue, I ping'ed 10.251.25.32-10.251.25.64 every minute for ten minutes, with the following result:

- `10.251.25.32`(=localhost) and `10.251.25.46`(the other mac mini) replied to all 10 pings
- `10.251.25.34` replied to three of the pings, `10.251.25.50` replied to two of the pings, and `10.251.25.52`, `10.251.25.53`, `10.251.25.58`, and `10.251.25.60` replied to one ping each.
- none of the others replied

So apparently something is failing with the network connectivity of the odroids.

    /data/misc/smsc95xx_mac_addr


# 2015-10-20 Tinkuy.dk and ClojureScript Infrastructure

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

# 2015-10-15 Library data

Preprocessed/analysed some library data, to build an app upon later on:

Results:

- Recommendations/related materials, based on similarity measure similar to cosine-similarity, but with slightly different weight for more relevant results.
- Eigenvector analysis (truncated SVD) of materials/loans, which can be used for a fast "relatedness" distance between materials (and will also be interesting to explore, to see if it can be used for a computational/empirical aproach to "genres")
- Easy to use data model (triples a la LOD) from different source, - and loaded into CouchDB
- Compressed local copy/backup of data

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

# 2015-10-04 Hack4DK

Participated in the yearly danish culural heritage hackathon:

Results:

- An embeddable viewer for 360º rotational images from the collection of the National Museum, which makes it easier for people to share these rotational images.
- Semantic markup of data from Det Danske Filminstitut, which will make the data discoverable by search-engines, robots, etc.

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
