(ns artis.cloud.oci.summarized-usages
  ""
  (:refer-clojure :exclude [get group-by])
  (:require [artis.cloud.oci.config :as config] 
            [artis.util :as util]
            [clojure.tools.logging :as log])
  (:import (com.oracle.bmc.usageapi.model RequestSummarizedUsagesDetails
                                          RequestSummarizedUsagesDetails$Granularity)
           (com.oracle.bmc.usageapi.requests RequestSummarizedUsagesRequest)
           (java.math BigDecimal))
  (:gen-class))

(def granularities #{:daily :hourly :monthly :total})

(defn ->summarized-usages-details-req
  "Takes `tenant-id`, `granularity`, `start-time`, `end-time`.
   Builds and returns an instance of `om.oracle.bmc.usageapi.model.RequestSummarizedUsagesDetails`
  
   Granularity MUST BE one of #{:daily :hourly :monthly :total}."
  ^RequestSummarizedUsagesDetails
  [tenant-id granularity start-time end-time {:keys [compartment-depth group-by] :as params}]
  (assert (contains? granularities granularity))
  (assert (instance? java.util.Date start-time))
  (assert (instance? java.util.Date end-time))

  (let [builder (RequestSummarizedUsagesDetails/builder)]
    (do
      (.tenantId builder tenant-id)

      (case granularity
        :hourly  (.granularity builder RequestSummarizedUsagesDetails$Granularity/Hourly) 
        :daily   (.granularity builder RequestSummarizedUsagesDetails$Granularity/Daily) 
        :monthly (.granularity builder RequestSummarizedUsagesDetails$Granularity/Monthly) 
        :total   (.granularity builder RequestSummarizedUsagesDetails$Granularity/Total) 
        :noop)

      (when-not (nil? compartment-depth)
        (.compartmentDepth builder (new BigDecimal compartment-depth)))

      (when-not (nil? group-by)
        (.groupBy builder group-by))

      ;(.groupBy builder ["compartmentName" "compartmentDepth"])
      ;(.groupBy builder ["region"])
      ;(.groupBy builder ["tagNamespace" "tagKey" "tagValue"])
      ;(.groupBy builder ["tagNamespace"])
      ;(.groupBy builder ["tenantId"])
     ; (.groupBy builder ["tenantName"])
      ;(.groupBy builder ["service"])
      ;(.groupBy builder ["skuName"])
      ;(.groupBy builder ["skuPartNumber"])
      ;(.groupBy builder ["logicalAd"])
      ;(.groupBy builder ["resourceId"])
      ;(.groupBy builder ["resourceName"])

      (.timeUsageStarted builder start-time)
      (.timeUsageEnded builder end-time)

      (.build builder))))

(defn ->summarized-usages-req
  "Takes `details-req`, optional `limit`, `page`. Builds and returns an instance of
   `com.oracle.bmc.usageapi.model.RequestSummarizedUsagesDetailsRequest`
  
   Yes, this really is how Oracle designed the API."
  ^RequestSummarizedUsagesRequest
  [details-req &
   {:keys [limit page opc-request-id]}]
  (let [builder (RequestSummarizedUsagesRequest/builder)]
    (do
      (.requestSummarizedUsagesDetails builder details-req)

      (when-not (nil? limit)
        (.limit builder (int limit)))

      (when-not (nil? page)
        (.page builder page))

      (when-not (nil? opc-request-id)
        (.opcRequestId builder opc-request-id))

      (.build builder))))

(defn- Tag->map
  "Takes `tag`, converts it to a Clojure map."
  [tag]
  {:namespace (.getNamespace tag)
   :key       (.getKey tag)
   :value     (.getValue tag)})

(defn- UsageSummary->map 
  "Takes `summary`, convert into a Clojure map.

   See https://docs.oracle.com/en-us/iaas/tools/java/3.62.0/com/oracle/bmc/usageapi/model/UsageSummary.html 
   for Java information."
  [^com.oracle.bmc.usageapi.model.UsageSummary summary]
  {:availability-domain (.getAd summary)
   :attributed-cost     (.getAttributedCost summary)
   :attributed-usage    (.getAttributedUsage summary)
   :compartment         {:id   (.getCompartmentId summary) 
                         :name (.getCompartmentName summary)
                         :path (.getCompartmentPath summary)}
   :computed-amount     (.getComputedAmount summary)
   :computed-quantity   (.getComputedQuantity summary)
   :currency            (.getCurrency summary)
   :forecast?           (.getIsForecast summary)
   :discount            (.getDiscount summary)
   :list-rate           (.getListRate summary)
   :overage             (.getOverage summary)
   :overages-flag       (.getOveragesFlag summary)
   :platform            (.getPlatform summary)
   :region              (.getRegion summary)
   :resource            {:id (.getResourceId summary) :name (.getResourceName summary)}
   :service             (.getService summary)
   :shape               (.getShape summary)
   :sku                 {:name (.getSkuName summary) :part-number (.getSkuPartNumber summary)}
   :subscription-id     (.getSubscriptionId summary)
   :tenant              {:id (.getTenantId summary) :name (.getTenantName summary)}
   :unit                (.getUnit summary)
   :unit-price          (.getUnitPrice summary)
   :weight              (.getWeight summary)
   :time-usage-started  (.getTimeUsageStarted summary)
   :time-usage-ended    (.getTimeUsageEnded summary)
   :tags                (vec (map Tag->map (.getTags summary)))})

(defn- UsageAggregation->map
  "Takes `aggregation`, an instance of com.oracle.bmc.usageapi.model.UsageAggregation,
   converts it into a Clojure map."
  [^com.oracle.bmc.usageapi.model.UsageAggregation aggregation]
  {:group-by (vec (.getGroupBy aggregation))
   :summaries (vec (map UsageSummary->map (.getItems aggregation)))})

(defn- RequestSummarizedUsagesResponse->map
  "Takes `response`, an instance of com.oracle.bmc.usageapi.responses.RequestSummarizedUsagesResponse, 
   converts it into a Clojure map."
  [^com.oracle.bmc.usageapi.responses.RequestSummarizedUsagesResponse response]
  {:opc   {:next-page (.getOpcNextPage response) :request-id (.getOpcRequestId response)}
   :usage-aggregation (UsageAggregation->map (.getUsageAggregation response))})

(defn get 
  "Takes `tenant-id`, `granularity`, `start-time`, `end-time`, returns a map with data or error."
  ([tenant-id granularity start-time end-time] (get tenant-id granularity start-time end-time {}))
  ([tenant-id granularity start-time end-time params]
    (let [client         (config/->usageapi-client)
          details-req    (->summarized-usages-details-req tenant-id granularity start-time end-time params)
          _              (log/debug details-req)
          opc-request-id (util/generate-string! 71 :prepend "soundhound-request-id...")
          _              (log/debug (format "Opc-Request-Id=[%s]" opc-request-id))
          req            (->summarized-usages-req details-req :limit 1000 :opc-request-id opc-request-id)]
      (try
        (->> (.requestSummarizedUsages client req)
             RequestSummarizedUsagesResponse->map 
             (assoc {} :data))
        (catch com.oracle.bmc.model.BmcException bmc-ex
          {:error (ex-info (.getMessage bmc-ex)
                           {}
                           bmc-ex)})
        (catch Exception ex
          (log/error (format "Unexpected exception type in artis.cloud.oci.summarized_usages %s"
                             (type ex)))
          {:error (ex-info (.getMessage ex)
                           {}
                           ex)})))))
