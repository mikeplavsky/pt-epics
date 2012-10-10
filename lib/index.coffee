exports.load = ->

  db = require("db").current()
  $ = require "jquery"

  db.getDoc "rtm", (err,doc) ->

    $("#rtm").text doc["projected date"]
    $("#actual").text doc["actual on"]

  db.getView "pt-epics", "burndown", (err,doc) ->
     for row in doc.rows
        $("<li/>").text("#{row.key}:#{row.value}").appendTo( "#burndown" )

