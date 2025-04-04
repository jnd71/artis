(ns artis.cloud.oci.config
  "Centralized createdion of the Oracle CLoud ConfigFileReader
   and ConfigFileAuthenticationDetailsProvider."
  (:import (com.oracle.bmc ConfigFileReader)
           (com.oracle.bmc.auth ConfigFileAuthenticationDetailsProvider)
           (com.oracle.bmc.identity IdentityClient)
           (com.oracle.bmc.usageapi UsageapiClient))
  (:gen-class))

(defn ->provider
  "Creates an instance of `ConfigFileAuthenticationDetailsProvider`."
  []
  (let [config-file-reader (ConfigFileReader/parseDefault)]
    (new ConfigFileAuthenticationDetailsProvider config-file-reader)))

(defn ->identity-client
  "Creates an instance of `com.oracle.bmc.identity.IdentityClient` for use with
   the IAM API."
  ([]         (->identity-client (->provider)))
  ([provider] (let [builder (IdentityClient/builder)]
                (.build builder provider)))) 

(defn ->usageapi-client
  "Creates an instance of `com.oracle.bmc.usageapi.UsageapiClient` for use with
   the Usage API."
  ([]         (->usageapi-client (->provider)))
  ([provider] (let [builder (UsageapiClient/builder)]
                (.build builder provider))))
