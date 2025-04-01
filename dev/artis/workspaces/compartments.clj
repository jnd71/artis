(ns artis.workspaces.compartments
  ""
  (:require [clojure.string :as string]
            [environ.core :refer [env]]
            [artis.cloud.oci.compartments :as compartments])
  (:import (java.time LocalDateTime ZoneId ZonedDateTime)
           (java.time.format DateTimeFormatter)))

(def tenancy-id (env :root-tenancy-id))
(def santa-clara-tz (ZoneId/of "America/Los_Angeles"))
(def rfc-1123-date-time (DateTimeFormatter/RFC_1123_DATE_TIME))

(defn list-all
  ""
  [root-id]
  (let [{:keys [data]} (compartments/list root-id {:include-subtree? true 
                                                   :access-level     :any 
                                                   :limit            1000})]
    (if (string/blank? (get-in data [:opc :next-page]))
      (:items data)
      (loop [xs        (:items data)
             next-page (get-in data [:opc :next-page])]
        ;(println (format "Starting loop, (count xs)=[%s], (last xs)=[%s], (string/blank? next-page)=[%s]."
        ;                 (count xs) (last xs) (string/blank? next-page)))
        (if (string/blank? next-page)
          xs 
          (let [inner-response (compartments/list root-id {:include-subtree? false 
                                                           :access-level     :any 
                                                           :limit            1000
                                                           :next-page        next-page})]
            (recur (into xs (get-in inner-response [:data :items])) 
                   (get-in inner-response [:data :opc :next-page]))))))))

(defn compartments->id-lookup 
  "Takes `cs`, `ids`, `root-id`, returns a map of {id name} for lookup on path build."
  [cs ids root-id]
  (let [compartment (first cs)]
    (if (nil? compartment)
      ;; for the final addition, we want to ensure the root tenancy is added
      (let [remote      (compartments/fetch root-id)
            remote-name (get-in remote [:data :compartment :name])]
        (assoc ids root-id {:name remote-name :parent-id nil}))
      (recur (rest cs)
             (assoc ids (:id compartment) {:name (:name compartment) :parent-id (:parent-id compartment)})
             root-id))))

(defn parent-id!
  "Takes id, `ids`, root-id. Returns the parent-id unless it is equal to the root-id, then it returns nil.

   This function is required because Oracle's API returns partial results with list, requiring an external
   check to retrieve the parent information for most parent-id == nil."
  [id ids root-id]
  (if (= id root-id)
    ;; because all compartments are downstream of the tenancy id, if the id is the same as the root-id, 
    ;; the tree root has been discovered and we have no parent
    nil
    ;; we hope and pray that we have the ID stored locally. If we don't, it's RPC time.
    (let [parent-id-from-local (:parent-id (get ids id))]
      (if-not (nil? parent-id-from-local)
        parent-id-from-local
        (let [parent-from-remote    (compartments/fetch id)
              parent-id-from-remote (get-in parent-from-remote [:data :compartment :parent-id])]
          ;(println "Made remote call to parent --> " parent-from-remote)
          parent-id-from-remote)))))

(defn compartment-name!
  "Takes id, `ids`. Returns the compartment name.

   This function is required because Oracle's API returns partial results with list, requiring an external
   check to retrieve the parent information for most parent-id == nil."
  [id ids]
  ;; we hope and pray that we have the name stored locally. If we don't, it's RPC time.
  (let [name-from-local (:name (get ids id))]
    (if-not (string/blank? name-from-local)
      name-from-local
      (let [parent-from-remote      (compartments/fetch id)
            parent-name-from-remote (get-in parent-from-remote [:data :compartment :name])]
        ;(println "compartment-name! remote call --> " parent-from-remote)
        parent-name-from-remote))))

(defn paths! 
  ""
  [id ids path root-id]
  (let [parent-id        (parent-id! id ids root-id)
        compartment-name (compartment-name! id ids)]
    (cond
      (and (string/blank? path) (nil? parent-id))       compartment-name
      (and (string/blank? path) (not (nil? parent-id))) (recur parent-id ids compartment-name root-id)
      (not (nil? parent-id))                            (recur parent-id ids (format "%s/%s"
                                                                                     compartment-name 
                                                                                     path)
                                                               root-id)
      :else (format "%s/%s" compartment-name path))))

(defn compartments->paths
  ""
  [cs ids paths root-id]
  (let [compartment (first cs)]
    (if (nil? compartment)
      paths
      (let [constructed-path (paths! (:id compartment) ids "" root-id)]
        (recur (rest cs) ids (conj paths constructed-path) root-id)))))

(defn hierarchy
  ""
  [root-id]
  (let [cs    (list-all root-id)
        ids   (compartments->id-lookup cs {} root-id)
        paths (compartments->paths cs ids [] root-id)]
    paths))

(defn format-date
  ""
  [date]
  (.format rfc-1123-date-time (ZonedDateTime/ofInstant (.toInstant date) santa-clara-tz)))

(defn compartments->csv
  [xs ids root-id]
  (loop [i 0
         v [["Name" "Path" "Description" "Lifecycle State" "Created At" "Tags"]]]
    (if (>= i (count xs))
      v
      (let [m          (get xs i)
            path       (paths! (:id m) ids "" root-id)
            created-at (format-date (:created-at m))]
        (recur (+ 1 i) (conj v [(:name m)
                                path
                                (:description m) 
                                (:lifecycle-state m) 
                                created-at
                                (:defined-tags m)]))))))

(defn table
  [root-id]
  (let [cs  (list-all root-id)
        ids (compartments->id-lookup cs {} root-id)
        csv (compartments->csv cs ids root-id)]
    csv))
        
