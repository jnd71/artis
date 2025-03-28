(ns artis.cloud.oci.tags
  ""
  (:refer-clojure :exclude [list])
  (:require [artis.cloud.oci.config :as config] 
            [camel-snake-kebab.core :as csk]
            [clojure.tools.logging :as log])
  (:import (com.oracle.bmc.identity.requests ListCostTrackingTagsRequest))
  (:gen-class))

(defn- ->list-tags-req
  "Takes `compartment-id` and optional `limit`, `page`. Builds and returns an instance of 
   `com.oracle.bmc.identity.requests.ListCostTrackingTagsRequest`."
  [compartment-id &
   {:keys [limit page]}]
  (let [builder (ListCostTrackingTagsRequest/builder)]
    (do
      (.compartmentId builder compartment-id)

      (when (not (nil? limit))
        (.limit builder limit)) 

      (when (not (nil? page))
        (.page builder page)))

    (.build builder)))

(defn- DefinedTags->map
  "takes `dts`, converts it to a Clojure Map."
  [dts]
  (loop [namespaces  {}
         ks          (keys dts)]
    (if (empty? ks)
      namespaces 
      (let [string-namespace-key  (first ks)
            keyword-namespace-key (csk/->kebab-case-keyword string-namespace-key)
            values                (get dts string-namespace-key)
            value                 (loop [tags     {}
                                         tag-keys (keys values)]
                                    (if (empty? tag-keys)
                                      tags
                                      (let [string-tag-key  (first tag-keys)
                                            keyword-tag-key (csk/->kebab-case-keyword string-tag-key)
                                            tag-key-value   (get values string-tag-key)]
                                        (recur (assoc tags keyword-tag-key tag-key-value) (rest tag-keys)))))]
        (recur (assoc namespaces keyword-namespace-key value) (rest ks))))))

(defn- Tag->map
  "Takes `tag`, convert into a Clojure map.

   See https://docs.oracle.com/en-us/iaas/tools/java/3.62.0/com/oracle/bmc/identity/model/Tag.html 
   for Java information."
  [^com.oracle.bmc.identity.model.Tag tag]
  {:id               (.getId tag)
   :compartment-id   (.getCompartmentId tag)
   :name             (.getName tag)
   :description      (.getDescription tag)
   :cost-tracking?   (.getIsCostTracking tag)
   :retired?         (.getIsRetired tag)
   :lifescycle-state (.getValue (.getLifecycleState tag))
   :created-at       (.getTimeCreated tag)
   :freeform-tags    (.getFreeformTags tag)
   :defined-tags     (DefinedTags->map (.getDefinedTags tag))
   :tag-namespace    {:id (.getTagNamespaceId tag) :name (.getTagNamespaceName tag)}})

(defn- ListTagNamespacesResponse->map
  "Takes `response`, an instance of com.oracle.bmc.identity.responses.ListCostTrackingTagsResponse,
   converts it into a Clojure map."
  [^com.oracle.bmc.identity.responses.ListCostTrackingTagsResponse response]
  {:opc   {:next-page (.getOpcNextPage response) :request-id (.getOpcRequestId response)}
   :items (vec (map Tag->map (.getItems response)))})

(defn list
  "Takes `compartment-id`, returns a map with data or error. Data is a list of tags."
  [compartment-id]
  (let [client (config/->identity-client)
        req    (->list-tags-req compartment-id)]
    (try
      (->> (.listCostTrackingTags client req)
           ListTagNamespacesResponse->map
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
