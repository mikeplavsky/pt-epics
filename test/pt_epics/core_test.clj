(ns pt-epics.core-test
  (:use clojure.test
        pt-epics.core))

(deftest test-date-to-long
  (testing "converting date to number of milliseconds"
    (is (= (to-long "1970/01/01")  0))
    (is (= (to-long "1970/01/01 00:00:01")  1000))))

(def project1 {:body "
<project>
  <iteration>
    <stories>
      <story>
        <labels> Dashboards,RESTs ,News </labels>
        <estimate>11</estimate>             
        <accepted_at>2012/05/14 11</accepted_at>             
      </story>
      <story>
        <labels>Dashboards,RESTs</labels>
        <estimate>12</estimate>             
        <accepted_at>2012/06/12 11</accepted_at>             
      </story>
      <story>
        <labels>RESTs</labels>
        <estimate>12</estimate>             
        <accepted_at>2012/07/14 11</accepted_at>             
      </story>
      <story>
        <labels>Policies</labels>
        <estimate>12</estimate>             
      </story>
      <story>
        <labels>Policies</labels>
        <estimate>10</estimate>             
      </story>
    </stories>
  </iteration>              
</project>              
"})

(deftest test-labels
  (testing "should get right labels"
    (let  [p (zip-project project1)]
      (is (= (get-labels p) #{"policies" "dashboards" "rests" "news"})))))

(deftest test-label-weight
  (testing "should get right weight"
    (let [p (zip-project project1) 
          s (get-stories p)]
      (is (= (label-weight "news" s) 11))
      (is (= (label-weight "rests" s) 35))
      (is (= (label-weight "dashboards" s) 23)))))      

(deftest test-burndown
  (testing "should get right decrements"
    (let [p [(zip-project project1)]
          s (stories p)]
      (is (= (burndown s) 12)))))


