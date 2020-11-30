(ns app.routes)

(def host "")

(def prefix "api/v1")

(defn get-patients-path [] (str host prefix "/patients"))

(defn get-delete-patient-path [id] (str host prefix "/patients/" id))
