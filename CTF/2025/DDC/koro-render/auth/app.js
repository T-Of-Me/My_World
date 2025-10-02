var express = require('express');
var mongoose = require('mongoose');

var indexRouter = require('./routes/index');

var app = express();

require('dotenv').config();

app.use(express.json());

// Database connection
mongoose.connect(process.env.MONGO_URI)
  .then(() => console.log('Connected to MongoDB'))
  .catch(err => console.error('Could not connect to MongoDB', err));

app.use('/', indexRouter);

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error('HTTP 404 Not Found');
  err.status = 404;
  next(err);
});

// error handler
app.use(function(err, req, res, next) {
  // render the error page
  res.status(err.status || 500);
  res.json(err);
});

module.exports = app;
