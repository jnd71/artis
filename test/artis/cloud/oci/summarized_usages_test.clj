(ns artis.cloud.oci.summarized-usages-test
  (:require [clojure.test :refer :all]
            [artis.cloud.oci.summarized-usages :refer :all])
  (:import (com.oracle.bmc.usageapi.model RequestSummarizedUsagesDetails
                                          RequestSummarizedUsagesDetails$Granularity)
           (java.time Instant)
           (java.time.temporal ChronoUnit)
           (java.util Date)))

(deftest ->summarized-usages-test
  (let [tenancy         "foo-bar-...000ax"
        granularity-key :total
        granularity     (RequestSummarizedUsagesDetails$Granularity/Total) 
        end             (Instant/now)
        start           (Date/from (.minus end 7 ChronoUnit/DAYS))
        end-date        (Date/from end)
        req             (->summarized-usages-req tenancy 
                                                 granularity-key 
                                                 start
                                                 end-date)]
  (testing "Builds a RequestSummarizedUsagesDetails with correct tenancy."
    (is (= tenancy (.getTenantId req))))
  (testing "Builds a RequestSummarizedUsagesDetails with correct granularity."
    (is (= granularity (.getGranularity req))))
  (testing "Builds a RequestSummarizedUsagesDetails with correct start date."
    (is (= start (.getTimeUsageStarted req))))
  (testing "Builds a RequestSummarizedUsagesDetails with correct end date."
    (is (= end-date (.getTimeUsageEnded req))))))

(deftest ->summarized-usages-req-granularity-assertion-test
  (testing "AssertionError thrown if granularity is a string"
    (is (thrown? AssertionError (->summarized-usages-req "id" "foo" "start" "end"))))
  (testing "AssertionError thrown if granularity is nil"
    (is (thrown? AssertionError (->summarized-usages-req "id" nil "start" "end"))))
  (testing "AssertionError thrown if granularity is a data structure"
    (is (thrown? AssertionError (->summarized-usages-req "id" {} "start" "end")))
    (is (thrown? AssertionError (->summarized-usages-req "id" [] "start" "end"))))
  (testing "AssertionError thrown if granularity is a number"
    (is (thrown? AssertionError (->summarized-usages-req "id" 7 "start" "end")))))

(deftest ->summarized-usages-req-usagetimes-assertion-test
  (testing "AssertionError thrown if start-time is a nil"
    (is (thrown? AssertionError (->summarized-usages-req "id" :total nil (Date/from (Instant/now))))))
  (testing "AssertionError thrown if start-time is a string"
    (is (thrown? AssertionError (->summarized-usages-req "id" :total "start" (Date/from (Instant/now))))))
  (testing "AssertionError thrown if start-time is a data structure"
    (is (thrown? AssertionError (->summarized-usages-req "id" :total {} (Date/from (Instant/now)))))
    (is (thrown? AssertionError (->summarized-usages-req "id" :total [] (Date/from (Instant/now))))))
  (testing "AssertionError thrown if start-time is a number"
    (is (thrown? AssertionError (->summarized-usages-req "id" :total 7 (Date/from (Instant/now))))))

  (testing "AssertionError thrown if end-time is a nil"
    (is (thrown? AssertionError (->summarized-usages-req "id" :total (Date/from (Instant/now)) nil))))
  (testing "AssertionError thrown if end-time is a string"
    (is (thrown? AssertionError (->summarized-usages-req "id" :total (Date/from (Instant/now)) "end"))))
  (testing "AssertionError thrown if end-time is a data structure"
    (is (thrown? AssertionError (->summarized-usages-req "id" :total (Date/from (Instant/now)) {})))
    (is (thrown? AssertionError (->summarized-usages-req "id" :total (Date/from (Instant/now)) []))))
  (testing "AssertionError thrown if end-time is a number"
    (is (thrown? AssertionError (->summarized-usages-req "id" :total (Date/from (Instant/now)) 7)))))
