(ns artis.cloud.oci.util
  "Utility functions for OCI."
  (:require [artis.util :refer [generate-string!]]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn generate-opc-request-id!
  "Generates and returns an OPC Request Id."
  []
  (let [opc-request-id (generate-string! 71 :prepend "soundhound.opc.request-id...")]
    (log/debug "generated-opc-request-id=" opc-request-id)
    opc-request-id))
