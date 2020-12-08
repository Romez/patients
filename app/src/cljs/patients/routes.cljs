(ns patients.routes
  (:require [clojure.string :refer [join]]))

(def host "")

(def prefix "/api/v1")

(defn make-query-string [m]
  (join "&" (for [[k v] m] (str (name k) "=" v))))

(defn get-patients-path [qp]
  (let [qs (make-query-string qp)]
    (if (empty? qs)
      (str host prefix "/patients")
      (str host prefix "/patients?" qs))))

(defn get-patient-path [id] (str host prefix "/patients/" id))
