(ns pt-epics.core)
(use '[clojure.data.zip.xml :only (attr text xml->)])
(require '[clj-http.client :as client]
         '[clojure.xml :as xml]
         '[clojure.zip :as zip])

(def pt-url 
  "https://www.pivotaltracker.com/services/v3/projects/%s/stories?type:feature,release")

(defn get-project-stories 
  [id]
  (client/get (format pt-url id)
  {:headers {"X-TrackerToken" "d85f22694013ecaa075b0e938f104854"}}))

(defn get-stream
  [s]
  (java.io.ByteArrayInputStream. (.getBytes s)))

(defn values 
  [loc,ks]
  (assoc (map #(list % (first (xml-> loc % text))) ks))) 

(defn get-labels 
  [z]
  (xml-> z :story [:labels] 
         #(values % [:labels :estimate :name])))

(def example 
"
  <stories>
    <story>
      <id type='Integer'>11</id>
      <state>Finished</state>
    </story>
    <story>
      <id type='Integer'>12</id>
      <state>Started</state>
    </story>
  </stories>
")

(def z1 (->
          example
          get-stream
          xml/parse
          zip/xml-zip))

;(def example-proj 246825)

(def z (-> 
         246825 
         get-project-stories 
         :body
         get-stream 
         xml/parse 
         zip/xml-zip))

(defn -main
  [& args]
  (println "Hello, World!"))
