(ns artis.cloud.oci.tag-defaults
  ""
  (:refer-clojure :exclude [list])
  (:require [artis.cloud.oci.config :as config] 
            [camel-snake-kebab.core :as csk]
            [clojure.tools.logging :as log])
  (:import (com.oracle.bmc.identity.requests ListTagDefaultsRequest))
  (:gen-class))

(defn- ->list-tag-defaults-req
  "Takes `compartment-id`, `params`, builds and returns an instance of
   `com.oracle.bmc.identity.requests.ListTagDefaultsRequest`."
  [compartment-id &
   {:keys [page limit]}]
  (let [builder (ListTagDefaultsRequest/builder)]
    (do
      (.compartmentId builder compartment-id)

      (when (not (nil? page))
        (.page builder page)) 

      (when (not (nil? limit))
        (.page builder limit)) 

    (.build builder))))

(defn- ResourceLock->map
  "Takes `lock`, converts it into a Clojure map.

   See https://docs.oracle.com/en-us/iaas/tools/java/3.62.0/com/oracle/bmc/identity/model/ResourceLock.html
   for the associated javadoc."
  [^com.oracle.bmc.identity.model.ResourceLock lock]
  {:active?             (.getIsActive lock)
   :message             (.getMessage lock)
   :related-resource-id (.getRelatedResourceId lock)
   :created-at          (.getTimeCreated lock)
   :lock-type           (.getValue (.getType lock))})

(defn- TagDefaultSummary->map
  "Takes `summary`, convert into a Clojure map.

   See https://docs.oracle.com/en-us/iaas/tools/java/3.62.0/com/oracle/bmc/identity/model/TagDefaultSummary.html 
   for Java information."
  [summary]
  {:id               (.getId summary)
   :compartment-id   (.getCompartmentId summary)
   :default-value    (.getValue summary)
   :required?        (.getIsRequired summary)
   :lifescycle-state (.getValue (.getLifecycleState summary))
   :tag              {:id           (.getTagDefinitionId summary)
                      :name         (.getTagDefinitionName summary)
                      :namespace-id (.getTagNamespaceId summary)}
   :locks            (->> (.getLocks summary)
                          (map ResourceLock->map)
                          vec)
   :created-at       (.getTimeCreated summary)})

(defn- ListTagsDefaultsResponse->map
  "Takes `response`, converts it into a Clojure map."
  [^com.oracle.bmc.identity.responses.ListTagDefaultsResponse response]
  {:opc   {:next-page (.getOpcNextPage response) :request-id (.getOpcRequestId response)}
   :items (->> (.getItems response)
               (map TagDefaultSummary->map)
               vec)})

(defn list
  "Takes `compartment-id`, returns a map with data or error. Data is a list of tag defaults."
  [compartment-id]
  (let [client (config/->identity-client)
        req    (->list-tag-defaults-req compartment-id)]
    (try
      (->> (.listTagDefaults client req)
           ListTagsDefaultsResponse->map
           (assoc {} :data))
      (catch com.oracle.bmc.model.BmcException bmc-ex
        {:error (ex-info (.getMessage bmc-ex)
                         {}
                         bmc-ex)})
      (catch Exception ex
        (log/error (format "Unexpected exception type in artis.cloud.oci.tag-namespace/list: %s"
                           (type ex)))
        {:error (ex-info (.getMessage ex)
                         {}
                         ex)}))))
