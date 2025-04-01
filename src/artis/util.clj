(ns artis.util
  ""
  (:require [clojure.string :refer [blank?]])
  (:import (org.apache.commons.lang3 RandomStringUtils))
  (:gen-class))

(def random-string-utils (RandomStringUtils/secure))

(defn generate-string! 
  "Takes `length`, optional `prepend`. Generates a random string of length."
  [length &
   {:keys [prepend]}]
  (let [string (.nextAlphanumeric random-string-utils length)]
    (if-not (blank? prepend)
      (format "%s%s" prepend string)
      string))) 

