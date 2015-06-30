require('http').createServer(function (req, res) {
  console.log(decodeURI(req.url.slice(2)));
  process.exit(+req.url.slice(1));
}).listen(7357, '127.0.0.1');
