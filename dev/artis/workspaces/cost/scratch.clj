(ns artis.workspaces.cost.scratch
  ""
  (:require [environ.core :refer [env]]
            [artis.cloud.oci.summarized-usages :as su])
  (:import (java.time LocalDateTime ZoneId ZonedDateTime)
           (java.util Date)))

(def tenancy-id (env :root-tenancy-id))

(def zone (ZoneId/of "America/Los_Angeles")) 
(def utc (ZoneId/of "UTC"))

;(def start (Date/from (.toInstant (ZonedDateTime/of 2025 3 13 0 0 0 0 utc))))
;(def end (Date/from (.toInstant (ZonedDateTime/of 2025 3 14 0 0 0 0 utc))))
(def start (Date/from (.toInstant (ZonedDateTime/of 2025 2 1 0 0 0 0 utc))))
(def end (Date/from (.toInstant (ZonedDateTime/of 2025 3 1 0 0 0 0 utc))))

(def demo-start (new Date "Sun Mar 16 00:00:00 UTC 2025"))
(def demo-end (new Date "Sun Mar 23 00:00:00 UTC 2025"))

(defn foo
  []
  "dev-foo")

(defn summarize
  ""
  [summary]
  (let [compartment (:compartment summary)]
    (->> (select-keys summary [:service :compartment]))))
         ;(assoc :compartment-name (:name compartment))
         ;(assoc :compartment-id   (:id compartment))
         ;(assoc :compartment-path (:path compartment))

         ;)))

(defn summarize-by-service
  [summary]
  (select-keys summary [:service :attributed-usage :attributed-cost :computed-cost :computed-usage
                        :time-usage-started :time-usage-ended]))

(defn summarize-by-region
  [summary]
  (select-keys summary [:region :attributed-usage :attributed-cost :computed-cost :computed-usage
                        :time-usage-started :time-usage-ended]))

(defn summarize-by-tags
  [summary]
  (select-keys summary [:tags :attributed-usage :attributed-cost :computed-cost :computed-usage
                        :time-usage-started :time-usage-ended]))

(defn summarize-by-tenancy
  [summary]
  (select-keys summary [:tenant :attributed-usage :attributed-cost :computed-cost :computed-usage
                        :time-usage-started :time-usage-ended]))

(defn summarize-by-sku
  [summary]
  (select-keys summary [:sku :attributed-cost :computed-cost :computed-usage
                        :time-usage-started :time-usage-ended]))

(defn summarize-by-ad
  [summary]
  (select-keys summary [:availability-domain :attributed-cost :computed-cost :computed-usage
                        :time-usage-started :time-usage-ended]))

(defn summarize-by-resource
  [summary]
  (select-keys summary [:resource :attributed-cost :computed-cost :computed-usage
                        :time-usage-started :time-usage-ended]))

(defn collect-tenancies
  "Takes `ss`, a list of summaries. Returns a set of tenant names."
  [ss s]
  (let [summary (first ss)
        tn (get-in summary [:tenant :name])]
    (println (format "collect-tenancies, tn=[%s], (count ss)=[%s]" tn (count ss)))
    (if (nil? summary)
      s
      (recur (conj s (get-in summary [:tenant :name])) (rest ss)))))
        

(defn tenancy-report
  ""
  []
  (let [summaries (get-in (su/get tenancy-id :monthly start end) [:data :usage-aggregation :summaries])
        tenancies (reduce (fn [coll tenancy]
                            (conj coll (get-in tenancy [:tenant :name])))
                          #{}
                          summaries)]
    summaries))


(defn sh-tags?
  "Takes `tags`, returns true if any tag has the namespace soundhound."
  [summary]
  (let [tags (:tags summary)]
    (loop [xs      tags
           sh-tag? false]
      (let [tag (first xs)]
        (cond
          (true? sh-tag?) sh-tag? ;; for short circuiting
          (nil? tag)      sh-tag? 
          :else           (let [found? (= "SoundHound" (:namespace tag))]
                            (recur (rest tags) found?)))))))

(defn summaries->tag-key-set
  "Takes `summaries`."
  [summaries tag-keys]
  (let [summary (first summaries)]
    (if (nil? summary)
      tag-keys
      (recur (rest summaries) (conj tag-keys (:key (first (:tags summary))))))))

(defn build-tag-values 
  ""
  [tag-key summaries]
  (loop [builder #{}
         xs      summaries]
    (let [summary (first xs)]
      (if (nil? summary)
        builder
        (let [summary-tag-key   (:key (first (:tags summary)))
              summary-tag-value (:value (first (:tags summary)))]
          (if (= tag-key summary-tag-key)
            (recur (conj builder summary-tag-value) (rest xs))
            (recur builder (rest xs))))))))

;(defn collate
;  [summaries]
;  (let [tag-keys (summaries->tag-key-set summaries #{})
;                 (reduce (fn [acc {:keys [tag-keys summar]
;    (

;(defn foo
;  ""
;  [summaries]
;  (let [tag-keys (summaries->tag-key-set summaries #{})
;        collated (reduce collate {} {:tag-keys :summaries summaries
;

(defn make-cost
  [tag-key tag-value summaries]
  (let [small (take 10 summaries)
        total (reduce (fn [acc summary]
                        (let [tag (first (:tags summary))]
                          (if (and (= tag-key (:key tag)) (= tag-value (:value tag)))
                            (let [computed-amount (if (nil? (:computed-amount summary))
                                                    0
                                                    (:computed-amount summary))]
                              (+ acc computed-amount)) 
                            acc)))
                      0
                      summaries)]
    total)) 


;artis.workspaces.costλ (make-cost "product" nil sh-summaries)
;442.500578726362M
;artis.workspaces.costλ (make-cost "product" "Stela" sh-summaries)
;0.429944844760M
;artis.workspaces.costλ (make-cost "product" "MusicSearch" sh-summaries)
;81316.867200627787M
;artis.workspaces.costλ (make-cost "product" "Agent Ops" sh-summaries)
;421.100513467015M
;artis.workspaces.costλ (make-cost "product" "ASR" sh-summaries)
;26502.993655590419M
;artis.workspaces.costλ (make-cost "product" "Amelia" sh-summaries)
;292.356509520615M
;artis.workspaces.costλ (make-cost "product" "Shephard" sh-summaries)
;0
;artis.workspaces.costλ (make-cost "product" "Sports and Entertainment" sh-summaries)
;361.212072180108M
;artis.workspaces.costλ (make-cost "product" "Transcription Service" sh-summaries)
;154.616353497347M
;artis.workspaces.costλ (make-cost "product" "Smart Answering" sh-summaries)
;3173.728993375756M
;artis.workspaces.costλ (make-cost "product" "Language Models" sh-summaries)
;135435.020885432861M
;artis.workspaces.costλ (make-cost "product" "Drive Thru" sh-summaries)
;1934.533545881876M
;artis.workspaces.costλ (make-cost "product" "Houndify" sh-summaries)
;25690.274080995492M
;artis.workspaces.costλ (make-cost "product" "Mobile Apps" sh-summaries)
;513.596998278952M
;artis.workspaces.costλ (make-cost "product" "NLU" sh-summaries)
;18636.916681119494M
;artis.workspaces.costλ (make-cost "product" "Voice AI Monetization" sh-summaries)
;0.131185529627M
;artis.workspaces.costλ (make-cost "product" "IoT" sh-summaries)
;1332.056230460104M
;artis.workspaces.costλ (make-cost "product" "Polaris" sh-summaries)
;1360.532744540025M
;artis.workspaces.costλ (make-cost "product" "Gmail" sh-summaries)
;233.92152M
;artis.workspaces.costλ (make-cost "product" "Web Tools" sh-summaries)
;6589.874130930191M
;artis.workspaces.costλ (make-cost "product" "Employee Assist" sh-summaries)
;684.135182947218M
;artis.workspaces.costλ (make-cost "product" "Smart Ordering" sh-summaries)
;0.000844311392M
;artis.workspaces.costλ (make-cost "product" "Other" sh-summaries)
;345.3384M
(def product-cost [["Product" "Cost"]
                   ["nil" 442.50]
                   ["Stela" 0.43]
                   ["MusicSearch" 81316.87]
                   ["Agent Ops" 421.10]
                   ["ASR" 26502.99]
                   ["Amelia" 292.36]
                   ["Shephard" 0]
                   ["Sports and Entertainment" 361.21]
                   ["Transcription Service" 154.62]
                   ["Smart Answering" 3173.73]
                   ["Language Models" 135435.02]
                   ["Drive Thru" 1934.54]
                   ["Houndify" 25690.27]
                   ["Mobile Apps" 513.60]
                   ["NLU" 18636.92]
                   ["Voice AI Monetization" 0.13]
                   ["IoT" 1332.06]
                   ["Polaris" 1360.53]
                   ["Gmail" 233.92]
                   ["Web Tools" 6589.87]
                   ["Employee Assist" 684.14]
                   ["Smart Ordering" 0.0008]
                   ["Other" 345.34]])
