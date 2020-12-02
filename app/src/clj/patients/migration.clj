(ns patients.migration
  (:require [ragtime.jdbc :as jdbc]
            [environ.core :refer [env]]
            [ragtime.repl :as repl]))

(defn load-config []
  {:datastore  (jdbc/sql-database {:dbtype "postgresql"
                                   :dbname (:db-name env)
                                   :host (:db-host env)
                                   :user (:db-user env)
                                   :password (:db-password env)})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate (load-config)))

(defn rollback []
  (repl/rollback (load-config)))
