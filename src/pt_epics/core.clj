(ns pt-epics.core)
(use '[clojure.data.zip.xml :only (attr text xml->)])
(require '[clj-http.client :as client]
         '[clojure.xml :as xml]
         '[clojure.zip :as zip]
         '[clojure.string :as str]
         '[clojure.contrib.string :as str1]
         '[clojure.set])

(def pt-url "https://www.pivotaltracker.com/services/v3/projects/%s/iterations")

(defn get-project-stories 
  [id]
  (client/get (format pt-url id)
  {:headers {"X-TrackerToken" (System/getenv "PT_API_TOKEN")}}))

(defn get-stream
  [s]
  (java.io.ByteArrayInputStream. (.getBytes s)))

(defn story 
  [loc,ks]
  (apply merge (map #(hash-map % (first (xml-> loc % text))) ks))) 

(defn get-stories 
  [z]
  (xml-> z :iteration :stories :story [:labels] 
         #(story % [:labels :estimate :accepted_at])))

(defn get-labels
  [z]
  (-> (map #(str/split (str/lower-case %) #",") (xml-> z :iteration :stories :story :labels text)) flatten set))

(defn zip-project
  [stories]
  (-> stories :body get-stream xml/parse zip/xml-zip))

(def project-ids [246825 454855 52499 78102 52476])

(defn pprojects 
  []
  (let [agents (map #(agent %) project-ids)
        task #(get-project-stories %)]
    (doall (map #(send-off % task) agents))
    (apply await agents)
    (map #(zip-project (deref %)) agents)))

(defn labels 
  [ps] 
  (apply clojure.set/union (map #(get-labels %) ps)))

(defn label-weight
  [label,stories]
  (reduce #(+ %1 
              (if (str1/substring? label (str/lower-case (:labels %2))) 
                (read-string (:estimate %2)) 
                0)) 
          0 stories))

(defn to-long
  [d]
  (.getTime (java.util.Date. d)))

(def start-time "2012/05/15")

(defn to-release
  [s]
  (filter #(if (:accepted_at %)
             (< (to-long start-time) 
              (to-long (first (str/split (:accepted_at %) #" "))))
             true) 
          s))

(defn epics 
  []
  (let [ps (pprojects)
        ls (labels ps)
        s (->> ps (map #(to-release (get-stories %))) flatten)]
  (apply merge 
         (map #(hash-map % (label-weight % s)) ls))))

(defn -main
  [& args]
  (doall (map #(println (get % 0) (get % 1)) (epics)))
  (shutdown-agents))
