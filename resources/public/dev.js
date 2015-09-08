// Code for running figwheel for linked javascript
(function() {
  var base = "http://" + location.hostname + ":3449/out/";
  function load(url, cb) {
    var e = document.createElement("script");
    document.head.appendChild(e);
    e.onload = cb;
    e.src = base + url;
  }
  load("goog/base.js", function() {
    goog.ENABLE_CHROME_APP_SAFE_SCRIPT_LOADING = true;
    load("goog/deps.js", function() {
      load("cljs_deps.js", function() {
        goog.require("figwheel.connect");
        goog.require("solsort.main");
      });
    });
  });
})();
