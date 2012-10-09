exports.load = ->
  db = require("db").current()
  $ = require "jquery"

  db.getView "pt-epics", "rtm", (err, data) ->
    $("#rtm").text data.rows[0].value.rtm
    $("#actual").text data.rows[0].value.actual

