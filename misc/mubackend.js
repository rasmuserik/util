var server = require('http').createServer();
var p2pserver = require('socket.io-p2p-server').Server
var io = require('socket.io')(server);
io.use(p2pserver);
server.listen(1234);

function randstr() { return Math.random().toString().slice(2); }

//{{{1 socket server + login
var daemon_room = randstr();
io.on('connection', function(socket) {
  require('request')({url: "http://localhost/db/_session",
    headers: {cookie: socket.handshake.headers.cookie}}, 
    function(_, _, data) {
      var username;
      try { username = JSON.parse(data).userCtx.name; } catch(e) {};
      if(username === "daemon") socket.join(daemon_room);
      io.sockets.to(daemon_room).emit("socket-connect", 
          {socket: socket.id, timestamp: +Date.now(), user: username});
      socket.on("http-response", http_response);
    });
  socket.on("disconnect", function() {
    io.sockets.to(daemon_room).emit("socket-disconnect", 
        {socket: socket.id, timestamp: +Date.now()});
  });
});

//{{{1 HTTP-server
var reqs = {};
server.on('request', function(req, res) {
  var url = req.url;
  if(url.slice(0,10) !== "/socket.io") {
    if(req.method === "GET") {
      if(reqs[url]) {
        reqs[url].res.push(res);
      } else {
        reqs[url] = {key: randstr(), res: [res]};
        io.sockets.to(daemon_room).emit("http-request", 
            {url: url, timestamp: +Date.now(), headers: req.headers, key: reqs[url].key });
      } 
    } else {
      res.end();
    }
  }
});
function http_response(o) {
  console.log("http-response", o);
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

