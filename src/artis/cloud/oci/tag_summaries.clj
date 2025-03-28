(ns artis.cloud.oci.tag-summaries
  ""
  (:refer-clojure :exclude [list])
  (:require [artis.cloud.oci.config :as config] 
            [camel-snake-kebab.core :as csk]
            [clojure.tools.logging :as log])
  (:import (com.oracle.bmc.identity.requests ListTagsRequest))
  (:gen-class))

(defn- ->list-tags-req
  "Takes `tag-ns-id` and optional `limit`, `page`. Builds and returns an instance of 
   `com.oracle.bmc.identity.requests.ListTagsRequest`."
  [tag-ns-id &
   {:keys [limit page]}]
  (let [builder (ListTagsRequest/builder)]
    (do
      (.tagNamespaceId builder tag-ns-id)

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

(defn- TagSummary->map
  "Takes `summary`, convert into a Clojure map.

   See https://docs.oracle.com/en-us/iaas/tools/java/3.62.0/com/oracle/bmc/identity/model/TagSummary.html 
   for Java information."
  [^com.oracle.bmc.identity.model.TagSummary summary]
  {:id               (.getId summary)
   :compartment-id   (.getCompartmentId summary)
   :name             (.getName summary)
   :description      (.getDescription summary)
   :cost-tracking?   (.getIsCostTracking summary)
   :retired?         (.getIsRetired summary)
   :lifescycle-state (.getValue (.getLifecycleState summary))
   :created-at       (.getTimeCreated summary)
   :freeform-tags    (.getFreeformTags summary)
   :defined-tags     (DefinedTags->map (.getDefinedTags summary))})

(defn- ListTagsResponse->map
  "Takes `response`, an instance of com.oracle.bmc.identity.responses.ListTagsResponse,
   converts it into a Clojure map."
  [^com.oracle.bmc.identity.responses.ListTagsResponse response]
  {:opc   {:next-page (.getOpcNextPage response) :request-id (.getOpcRequestId response)}
   :items (vec (map TagSummary->map (.getItems response)))})

(defn list
  "Takes `tag-ns-id`, returns a map with data or error. Data is a list of tags."
  [tag-ns-id]
  (let [client (config/->identity-client)
        req    (->list-tags-req tag-ns-id)]
    (try
      (->> (.listTags client req)
           ListTagsResponse->map
           (assoc {} :data))
      (catch com.oracle.bmc.model.BmcException bmc-ex
        {:error (ex-info (.getMessage bmc-ex)
                         {}
                         bmc-ex)})
      (catch Exception ex
        (log/error (format "Unexpected exception type in artis.cloud.oci.tag-summaries%s"
                           (type ex)))
        {:error (ex-info (.getMessage ex)
                         {}
                         ex)}))))
