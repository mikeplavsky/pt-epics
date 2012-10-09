exports.load = ->

  db = require("db").current()
  $ = require "jquery"

  db.getDoc "rtm", (err,doc) ->

    $("#rtm").text doc["projected date"]
    $("#actual").text doc["actual on"]

