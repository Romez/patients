(ns patients.routes)

(def host "")

(def prefix "/api/v1")

(defn get-patients-path [] (str host prefix "/patients"))

(defn get-patient-path [id] (str host prefix "/patients/" id))
