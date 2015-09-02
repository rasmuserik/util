var app = require('express')();
var proxy = require('express-http-proxy');
var server = require('http').Server(app);
server.listen(1234);

function randstr() { return Math.random().toString().slice(2); }
//{{{ auth
var authTokens = {};
function authToken(cookies) {
  var match = /AuthSession=([^;]*)/.exec(cookies);
  return match && match[1];
}
//{{{1 '/db' elastic search proxy, only accessible for daemon
app.use('/es', proxy('localhost:9200', {
  filter: function(req, res) { return authTokens[authToken(req.headers.cookie)]; },
  forwardPath: function(req, res) { return req.url; }
}));

//{{{1 '/db' couchdb proxy
app.use('/db', proxy('localhost:5984', {
  forwardPath: function(req, res) { return req.url; }
}));

//{{{1 '/socket.io' socket server + login
var io = require('socket.io')(server);
var p2pserver = require('socket.io-p2p-server').Server
io.use(p2pserver);

var daemon_room = randstr();
io.on('connection', function(socket) {
  require('request')({url: "http://localhost/db/_session",
    headers: {cookie: socket.handshake.headers.cookie}}, 
    function(_, _, data) {
      var username;
      try { username = JSON.parse(data).userCtx.name; } catch(e) {};
      console.log('connect', username);
      if(username === "daemon") {
        socket.join(daemon_room);
        authTokens[authToken(socket.handshake.headers.cookie)] = true;
      }
      io.sockets.to(daemon_room).emit("socket-connect", 
          {socket: socket.id, timestamp: +Date.now(), user: username});
      socket.on("http-response", http_response);
    });
  socket.on("disconnect", function() {
    io.sockets.to(daemon_room).emit("socket-disconnect", 
        {socket: socket.id, timestamp: +Date.now()});
  });
});

//{{{1 '*' Forward http-requests to daemon
var reqs = {};
app.get("*", function(req, res) {
  var url = req.url;
  if(reqs[url]) {
    reqs[url].res.push(res);
  } else {
    reqs[url] = {key: randstr(), res: [res]};
    io.sockets.to(daemon_room).emit("http-request", 
        {url: url, timestamp: +Date.now(), headers: req.headers, key: reqs[url].key});
  } 
});
function http_response(o) {
  var req = (("object" === typeof o) && o.url && reqs[o.url]) || {key: randstr()};
  if(req.key === o.key) {
    while(req.res.length) {
      var res = req.res.pop();
      res.writeHead(o.statusCode || 200, o.headers || {});
      res.end(o.content || "", o.encoding || "utf8"); 
    }
    reqs[o.url] = null; 
    io.sockets.to(daemon_room).emit("http-response-log", 
        { url: o.url, timestamp: +Date.now()});
  }
}

