(ns pt-epics.core)
(require '[clj-http.client :as client])

(def pt-url 
  "https://www.pivotaltracker.com/services/v3/projects/%s/stories?type:feature,release")

(defn get-project-stories 
  [id]
  (client/get (format pt-url id)
  {:headers {"X-TrackerToken" "d85f22694013ecaa075b0e938f104854"}}))

(defn -main
  [& args]
  (println "Hello, World!"))
