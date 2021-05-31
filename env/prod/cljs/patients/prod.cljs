(ns app.prod
  (:require
   [patients.core :as core]
   [cljsjs.sentry-browser]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(.init js/Sentry (clj->js {:dsn "https://0c92aba87d8442beaf041da819848aae@o359033.ingest.sentry.io/5547920"}))

(core/init!)
