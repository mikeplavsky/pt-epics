(ns pt-epics.core)
(use '[clojure.data.zip.xml :only (attr text xml->)]
     '[clojure.pprint])
(require '[clj-http.client :as client]
         '[clojure.xml :as xml]
         '[clojure.zip :as zip]
         '[clojure.string :as str]
         '[clojure.contrib.string :as str1]
         '[clojure.set]
         '[clojure.pprint]
         '[com.ashafa.clutch :as clutch])
(import 'java.util.Date)

(def pt-url "https://www.pivotaltracker.com/services/v3/projects/%s/iterations")
(def project-ids [246825 454855 52499 78102 52476])
(def ^:dynamic start-time "2012/05/15")
(def api-token (System/getenv "PT_API_TOKEN"))
(def pt-db "http://localhost:8001/pt")

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
  (.getTime (Date. d)))

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
        total (reduce #(+ %1 (read-string (:estimate %2))) 0 stories)]
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
  [projects]
  (let [ls (labels projects)
        s (stories projects)]
  (reduce #(conj %1 {%2 (label-weight %2 s)}) {} ls)))

(defn update-document
  [id doc]
  (let [prev (clutch/get-document pt-db id)
        rev (:_rev prev)
        d (assoc 
            (if rev (assoc doc :_rev rev) 
              doc) 
            :_id id)]
    (clutch/put-document pt-db 
                         (assoc d "actual on" (.toString (Date.))))))

(defn -main
  [& args]
  (let [pt-db "http://localhost:8001/pt"
        ps (pprojects)
        b (-> ps stories burndown)
        delta (- 
                   (-> b last first to-long) 
                   (-> b first first to-long)) 
        done (- (-> b first last) (-> b last last))
        left (- (-> b first last) done)
        left-ms (-> delta (/ done) (* left) long)
        rtm (Date. (+ (.getTime (Date.)) left-ms))]
    (update-document "epics" (-> ps epics))
    (update-document "burndown" (->> b flatten (apply hash-map)))
    (update-document "rtm" {"projected date" (.toString rtm)})
    (-> ps epics pprint)
    (->> b flatten (apply sorted-map) pprint)
    (println "Projected RTM: " (.toString rtm)))
  (shutdown-agents))
