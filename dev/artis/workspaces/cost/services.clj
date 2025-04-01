(ns artis.workspaces.cost.services
  ""
  (:require [artis.cloud.oci.summarized-usages :as su]
            [artis.writers :as writers]
            [camel-snake-kebab.core :as csk]
            [clojure.tools.logging :as log]
            [clojure.set :refer [rename-keys]]
            [environ.core :refer [env]])
  (:import (java.time LocalDateTime ZoneId ZonedDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Date Locale)))

(def tenancy-id (env :root-tenancy-id))

(def utc (ZoneId/of "UTC"))
(def formatter (.withZone (.withLocale (DateTimeFormatter/ofPattern "YYYY-MM-dd") (Locale/US)) utc))

(def start (Date/from (.toInstant (ZonedDateTime/of 2025 2 1 0 0 0 0 utc))))
(def end (Date/from (.toInstant (ZonedDateTime/of 2025 4 1 0 0 0 0 utc))))

(def params {:compartment-depth 6 :group-by ["service"]})

(defn services-response!
  "Returns a services response based on the defs above."
  []
  (su/get tenancy-id :daily start end params))

(defn collate-service
  "Takes `service`, `summaries`, returns collated data."
  [service summaries]
  (->> (filter #(= service (:service %)) summaries)
       (map #(select-keys % [:service :computed-amount :time-usage-started]))
       (map #(rename-keys % {:computed-amount :cost, :time-usage-started :date}))
       (sort-by :date)
       vec))

(defn collate-services
  "Takes `services, `summaries`, `collated`, returns the collated data."
  [services summaries collated]
  (let [service          (first services)
        collated-service (collate-service service summaries)]
    (if (nil? service)
      collated
      (recur (rest services) summaries (conj collated collated-service))))) 

(defn format-date
  "Takes `date`, returns a YYYY-MM-dd formatted date, or and empty string if an exception is thrown."
  [{:keys [date]}]
  (try
    (.format formatter (.toInstant date))
    (catch Exception ex
      (log/error ex)
      "")))

(defn format-service-for-csv
  "Takes `service-map`, returns a three-tuple vector for CSV with a formatted date."
  [service-map]
  (let [cost (if (nil? (:cost service-map)) 0 (:cost service-map))
        date (format-date service-map)]
    [(:service service-map) cost date]))

(defn format-services-for-csv
  [services]
  (vec (map format-service-for-csv services)))

(defn summaries->services-set
  [summaries services-set]
  (reduce #(conj %1 (:service %2)) services-set summaries))


(defn format-service
  [service summaries]
  (let [collated (collate-service service summaries)
        data     (format-services-for-csv collated)]
    (into [["Service" "Cost" "Date"]] data)))

(defn pull-and-write
  ""
  [compartment-id]
  (let [summaries   (get-in (services-response!) [:data :usage-aggregation :summaries])
        service-set (reduce #(conj %1 (:service %2)) #{} summaries)]
    (loop [ss (reduce #(conj %1 (:service %2)) #{} summaries)]
      (let [service (first ss)]
        (when-not (nil? service)
          (let [data (format-service service summaries)
                file (format "data/cost/services/%s-02-01-04-01.csv" (csk/->kebab-case-string service))]
            (writers/csv! data file)
            (recur (rest ss))))))))

