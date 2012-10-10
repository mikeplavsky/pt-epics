exports.views =
  burndown:
    map: (doc)->
      if doc._id == "burndown"
        for key,val of doc
          if key not in ["_id", "_rev", "actual on"]
            emit key, val

