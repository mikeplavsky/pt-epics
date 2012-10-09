exports.views =
  rtm:
    map: (doc)->
      if doc._id == "rtm"
        emit null,
          rtm: doc["projected date"]
          actual: doc["actual on"]
