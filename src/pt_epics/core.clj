(ns pt-epics.core
  (:gen-class)) 

(use '[clojure.data.zip.xml :only (attr text xml->)])
(require '[clj-http.client :as client]
         '[clojure.xml :as xml]
         '[clojure.zip :as zip]
         '[clojure.string :as str]
         '[clojure.set])

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

(def project-ids [246825 454855 52499 78102 52476])

(defn projects [] (map #(zip-project %) project-ids))

(defn pprojects 
  []
  (let [agents (map #(agent %) project-ids)]
    (doall (map #(send-off % (fn [id] (zip-project id))) agents))
    (apply await agents)
    (map #(deref %) agents)))

(defn labels 
  [ps] 
  (apply clojure.set/union (map #(get-labels %) ps)))

(defn -main
  [& args]
  (println (labels (pprojects)))
  (shutdown-agents))
