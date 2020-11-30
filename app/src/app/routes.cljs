(ns app.routes)

(def host "")

(def prefix "api/v1")

(defn get-patients-path [] (str host prefix "/patients"))
