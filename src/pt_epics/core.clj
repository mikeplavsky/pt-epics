(ns pt-epics.core)
(use '[clojure.data.zip.xml :only (attr text xml->)])
(require '[clj-http.client :as client]
         '[clojure.xml :as xml]
         '[clojure.zip :as zip]
         '[clojure.string :as str]
         '[clojure.contrib.string :as str1]
         '[clojure.set]
         '[clojure.pprint])

(def pt-url "https://www.pivotaltracker.com/services/v3/projects/%s/iterations")
(def project-ids [246825 454855 52499 78102 52476])
(def start-time "2012/05/15")
(def api-token (System/getenv "PT_API_TOKEN"))

(defn get-project-stories 
  [id]
  (client/get (format pt-url id)
  {:headers {"X-TrackerToken" api-token}}))

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

(defn split-labels
  [labels]
  (map str/trim (str/split (str/lower-case labels) #",")))

(defn get-labels
  [z]
  (-> (map 
        #(split-labels %)
        (xml-> z :iteration :stories :story :labels text)) 
    flatten 
    set))

(defn zip-project
  [stories]
  (-> stories :body get-stream xml/parse zip/xml-zip))

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
              (if (some #{label} (split-labels (:labels %2))) 
                (read-string (:estimate %2)) 
                0)) 
          0 stories))

(defn to-long
  [d]
  (.getTime (java.util.Date. d)))

(defn just-date 
  [story]
  (first (str/split (:accepted_at story) #" ")))

(defn from-rtm
  [stories]
  (filter #(if (:accepted_at %)
             (< (to-long start-time) 
              (to-long (just-date %)))
             true) 
          stories))

(defn estimate
  [story]
  (read-string (:estimate story)))

(defn incs
  [stories]
   (reduce 
     #(conj %1 
            (let [d (just-date %2)
                  e (estimate %2)
                  prev (get %1 d)] 
               {d (if prev (+ e prev) e)}))
     {} stories))

(defn burndown
  [stories]
  (let [accepted (filter #(:accepted_at %) stories)
        total (reduce #(+ %1 (read-string (:estimate %2))) 0 stories)
        current (fn [l,c] (- (:last l) (get c 1)))]
    (-> 
      (reduce #(conj %1 
                   (let [c (- (:last %1) (get %2 1))]  
                      {(get %2 0) c :last c}))
            {:last total start-time total} 
            (sort (incs accepted)))
       (dissoc :last)
       sort)))

(defn stories 
  [projects]
  (->> projects 
      (map #(from-rtm (get-stories %))) 
      flatten))

(defn epics 
  []
  (let [ps (pprojects)
        ls (labels ps)
        s (stories ps)]
  (reduce #(conj %1 {%2 (label-weight %2 s)}) {} ls)))

(defn -main
  [& args]
  (clojure.pprint/pprint (epics))
  (shutdown-agents))
