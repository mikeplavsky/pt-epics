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
        <labels>
          Dashboards,RESTs,News
        </labels>
      </story>
      <story>
        <labels>
          Dashboards,RESTs
        </labels>
      </story>
    </stories>
  </iteration>              
</project>              
"})

(deftest test-labels
  (testing "should get right labels"
    (let  [p (zip-project project1)]
      (is (= (get-labels p) #{"dashboards" "rests" "news"})))))

