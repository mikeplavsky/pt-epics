(ns pt-epics.core)
(use '[clojure.data.zip.xml :only (attr text xml->)])
(require '[clj-http.client :as client]
         '[clojure.xml :as xml]
         '[clojure.zip :as zip]
         '[clojure.string :as str])

(def pt-url 
  "https://www.pivotaltracker.com/services/v3/projects/%s/stories?type:feature,release")

(defn get-project-stories 
  [id]
  (client/get (format pt-url id)
  {:headers {"X-TrackerToken" "d85f22694013ecaa075b0e938f104854"}}))

(defn get-stream
  [s]
  (java.io.ByteArrayInputStream. (.getBytes s)))

(defn story 
  [loc,ks]
  (apply merge (map #(hash-map % (first (xml-> loc % text))) ks))) 

(defn get-stories 
  [z]
  (xml-> z :story [:labels] 
         #(story % [:labels :estimate :name])))

(defn get-labels
  [z]
  (-> (map #(str/split % #",") (xml-> z :story :labels text)) flatten set))

(defn get-labels-map
  [labels]
  (->> (map #(list % {}) labels) flatten (apply hash-map)))

(defn zip-project
  [id]
  (-> id get-project-stories :body get-stream xml/parse zip/xml-zip))

(def projects (pmap #(zip-project %) [246825 454855 52499 78102 52476]))

(def labels 
  [ps] 
  (apply clojure.set/union (map #(get-labels %) ps)))

(defn -main
  [& args]
  (get-labels z))
