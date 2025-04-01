(ns artis.workspaces.dev
  ""
  (:require [environ.core :refer [env]]))

(def tenancy-id (env :root-tenancy-id))

