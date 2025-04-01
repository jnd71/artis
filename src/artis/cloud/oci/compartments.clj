(ns artis.cloud.oci.compartments
  ""
  (:refer-clojure :exclude [list])
  (:require [artis.cloud.oci.config :as config] 
            [camel-snake-kebab.core :as csk]
            [clojure.tools.logging :as log])
  (:import (com.oracle.bmc.identity.requests GetCompartmentRequest
                                             ListCompartmentsRequest 
                                             ListCompartmentsRequest$AccessLevel))
  (:gen-class))

(defn ->list-compartments-req
  "Takes `compartment-id`, optional `limit`, `include-subtree?`, `page`, `access-level`. 
   Builds and returns an instance of `com.oracle.bmc.identity.requests.ListCompartmentsRequest`.
  
   The request AccessLevel is set via `access-level` if the value is :any or :accessible. Any
   other value, or nil, and AccessLevel is left unset."
  [compartment-id &
   {:keys [include-subtree? limit page access-level]}]
  (let [builder (ListCompartmentsRequest/builder)]
    (do
      (.compartmentId builder compartment-id)

      (if (true? include-subtree?)
        (.compartmentIdInSubtree builder true) 
        (.compartmentIdInSubtree builder false))

      (when (not (nil? page))
        (.page builder page))

      (when (not (nil? limit))
        (.limit builder (int limit)))
      
      (case access-level
        :any        (.accessLevel builder ListCompartmentsRequest$AccessLevel/Any)
        :accessible (.accessLevel builder ListCompartmentsRequest$AccessLevel/Accessible)
        :noop))

    (let [request (.build builder)]
      (log/debug (format "ListCompartmentsRequest=[%s]" (.toString request)))
      request)))

(defn ->get-compartment-req
  "Takes `compartment-id`, builds and returns an instance of 
   `com.oracle.bmc.identity.requests.GetCompartmentRequest`."
  [compartment-id]
  (let [builder (GetCompartmentRequest/builder)]
    
    (do
      (.compartmentId builder compartment-id))

    (let [request (.build builder)]
      (log/debug (format "GetCompartmentRequest=[%s]" (.toString request)))
      request)))

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

(defn- Compartment->map 
  "Takes `summary`, convert into a Clojure map.

   See https://docs.oracle.com/en-us/iaas/tools/java/3.62.0/com/oracle/bmc/identity/model/Compartment.html 
   for Java information."
  [^com.oracle.bmc.identity.model.Compartment compartment]
  {:id              (.getId compartment)
   :parent-id       (.getCompartmentId compartment)
   :name            (.getName compartment)
   :description     (.getDescription compartment)
   :accessible?     (.getIsAccessible compartment)
   :lifecycle-state (.getValue (.getLifecycleState compartment))
   :inactive-status (.getInactiveStatus compartment)
   :created-at      (.getTimeCreated compartment)
   :defined-tags    (DefinedTags->map (.getDefinedTags compartment))
   :freeform-tags   (.getFreeformTags compartment)})

(defn- ListCompartmentsResponse->map
  "Takes `response`, an instance of com.oracle.bmc.identity.responses.ListCompartmentsResponse,
   converts it into a Clojure map."
  [^com.oracle.bmc.identity.responses.ListCompartmentsResponse response]
  {:opc   {:next-page (.getOpcNextPage response) :request-id (.getOpcRequestId response)}
   :items (vec (map Compartment->map (.getItems response)))})

(defn- GetCompartmentResponse->map
  "Takes `response`, an instance of com.oracle.bmc.identity.responses.GetCompartmentResponse,
   converts it into a Clojure map."
  [^com.oracle.bmc.identity.responses.GetCompartmentResponse response]
  {:opc-request-id (.getOpcRequestId response)
   :compartment    (Compartment->map (.getCompartment response))})

(defn list
  "Takes `compartment-id`, returns a map with data or error. Data is a list of compartments."
  ([compartment-id] (list compartment-id {}))
  ([compartment-id {:keys [include-subtree? access-level page limit]}]
   (let [client (config/->identity-client)
         req    (->list-compartments-req compartment-id 
                                         :include-subtree? include-subtree? 
                                         :access-level     access-level 
                                         :page             page
                                         :limit            limit)]
     (try
       (->> (.listCompartments client req)
            ListCompartmentsResponse->map
            (assoc {} :data))
       (catch com.oracle.bmc.model.BmcException bmc-ex
         {:error (ex-info (.getMessage bmc-ex)
                          {}
                          bmc-ex)})
       (catch Exception ex
         (log/error (format "Unexpected exception type in artis.cloud.oci.compartments %s"
                            (type ex)))
         {:error (ex-info (.getMessage ex)
                          {}
                          ex)})))))

(defn fetch
  "Takes `compartment-id`, returns a map with data or error. Data is a compartment."
  [compartment-id]
   (let [client (config/->identity-client)
         req    (->get-compartment-req compartment-id)]
     (try
       (->> (.getCompartment client req)
            GetCompartmentResponse->map
            (assoc {} :data))
       (catch com.oracle.bmc.model.BmcException bmc-ex
         {:error (ex-info (.getMessage bmc-ex)
                          {}
                          bmc-ex)})
       (catch Exception ex
         (log/error (format "Unexpected exception type in artis.cloud.oci.compartments %s"
                            (type ex)))
         {:error (ex-info (.getMessage ex)
                          {}
                          ex)}))))
