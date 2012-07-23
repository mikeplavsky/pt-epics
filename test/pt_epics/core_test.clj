(ns pt-epics.core-test
  (:use clojure.test
        pt-epics.core))

(deftest date-to-long
  (testing "converting date to number of milliseconds"
    (is (= (to-long "1970/01/01")  0))
    (is (= (to-long "1970/01/01 00:00:01")  1000))))
