(ns artis.writers
  (:require [cheshire.core :refer :all]
            [clojure.data.csv :as csv] 
            [clojure.java.io :as io])
  (:gen-class))

(defn json!
  "Takes `d`, the data, and `file`, the file name to write to. Converts the data to a string and writes the JSON."
  [d file]
  (generate-stream d (clojure.java.io/writer file)))

(defn csv!
  [data file-name]
  (with-open [writer (io/writer file-name)]
    (csv/write-csv writer data)))
