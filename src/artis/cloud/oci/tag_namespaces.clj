(ns artis.cloud.oci.tag-namespaces
  ""
  (:refer-clojure :exclude [list])
  (:require [artis.cloud.oci.config :as config] 
            [camel-snake-kebab.core :as csk]
            [clojure.tools.logging :as log])
  (:import (com.oracle.bmc.identity.requests ListTagNamespacesRequest))
  (:gen-class))

(defn- ->list-tag-ns-req
  "Takes `tenancy-id`, `params`, builds and returns an instance of
   `com.oracle.bmc.identity.requests.ListTagNamespaceRequest`."
  [tenancy-id]
  (let [builder (doto (ListTagNamespacesRequest/builder)
                      (.compartmentId tenancy-id))]
    (.build builder)))

(defn- TagNamespaceSummary->map
  "Takes `summary`, convert into a Clojure map.

   See https://docs.oracle.com/en-us/iaas/tools/java/3.62.0/com/oracle/bmc/identity/model/TagNamespaceSummary.html
   for Java information."
  [summary]
  {:id               (.getId summary)
   :compartment-id   (.getCompartmentId summary)
   :name             (.getName summary)
   :description      (.getDescription summary)
   :retired?         (.getIsRetired summary)
   :lifescycle-state (.getValue (.getLifecycleState summary))
   :created-at       (.getTimeCreated summary)
   :freeform-rags    (.getFreeformTags summary)
   })

(defn- ListTagNamespacesResponse->map
  "Takes `response`, converts it into a Clojure map."
  [response]
  {:opc   {:next-page (.getOpcNextPage response) :request-id (.getOpcRequestId response)}
   :items (vec (map TagNamespaceSummary->map (.getItems response)))})

(defn list
  "Takes `tenancy-id`, returns a list of tag-namespaces."
  [tenancy-id]
  (let [client (config/->identity-client)
        req    (->list-tag-ns-req tenancy-id)]
    (try
      (->> (.listTagNamespaces client req)
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
