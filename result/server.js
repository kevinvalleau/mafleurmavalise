const express = require('express'),
    async = require('async'),
    pg = require('pg'),
    { Pool } = require('pg'),
    path = require('path'),
    cookieParser = require('cookie-parser'),
    bodyParser = require('body-parser'),
    methodOverride = require('method-override'),
    app = express(),
    server = require('http').Server(app),
    io = require('socket.io')(server);

io.set('transports', ['polling']);

const port = process.env.PORT || 4000;

io.sockets.on('connection', function (socket) {

  socket.emit('message', { text : 'Welcome!' });

  socket.on('subscribe', function (data) {
    socket.join(data.channel);
  });
});

const pool = new pg.Pool({
  connectionString: 'postgres://postgres:postgres@db/postgres'
});

async.retry(
  {times: 3, interval: 1000},
  function(callback) {
    pool.connect(function(err, client, done) {
      if (err) {
        console.log("Waiting for db");
      }
      callback(err, client);
    });
  },
  function(err, client) {
    if (err) {
      const dummy_result = [{ 'vote':'fleur', 'name': 'toto', 'date' : '20200730', 'id': '1'},{'vote':'valise', 'name': 'tutu', 'date' : '20200730', 'id': '2'}]
      io.sockets.emit("scores", JSON.stringify(dummy_result));
      console.log('db connection error');
      return console.error("Giving up - sending dummy data");
    }
    console.log("Connected to db");
    getVotes(client);
  }
);

/**
 * Méthodes qui envoie la requête à Postgresql
 * @param {PoolClient} client 
 */
function getVotes(client) {
  client.query('SELECT vote, id, name, date  FROM votes ORDER BY name', [], function(err, result) {
    if (err) {
      console.log("Error performing query: " + err);
    } else {
      console.log(result)
      // Ici se trouve l'émission du endpoint "scores issu de la requête de postgresql"
      io.sockets.emit("scores", JSON.stringify(result));
    }

    setTimeout(function() {getVotes(client) }, 1000);
  });
}


app.use(cookieParser());
app.use(bodyParser());
app.use(methodOverride('X-HTTP-Method-Override'));
app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
  res.header("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS");
  next();
});

app.use(express.static(__dirname + '/views'));

app.get('/', function (req, res) {
  res.sendFile(path.resolve(__dirname + '/views/index.html'));
});

server.listen(port, function () {
  const port = server.address().port;
  console.log('App running on port ' + port);
});
