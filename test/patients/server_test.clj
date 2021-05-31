(ns patients.server-test
  (:require [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [patients.migration :refer [migrate]]
            [patients.server :refer [app]]
            [patients.utils :refer [unparse-date]]
            [environ.core :refer [env]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honeysql.core :as sql]))

(def db-spec {:dbtype "postgresql"
              :dbname "patients_test"
              :host (:db-host env)
              :port (:db-port env)
              :user (:db-user env)
              :password (:db-password env)
              :stringtype "unspecified"})
(def db-options {:builder-fn rs/as-unqualified-maps
                 :return-keys true})

(def ^:dynamic *db* nil)

(defn with-connection [fn]
  (jdbc/with-transaction [tx (jdbc/get-datasource db-spec) {:rollback-only true}]
    (binding [*db* tx]
      (fn))))

(defn fix-migrate [fn]
  (migrate)
  (fn))

(use-fixtures :once fix-migrate)
(use-fixtures :each with-connection)

(deftest test-app-200
  (testing "check main page success"
    (let [{:keys [status]} ((app {:db *db*})
                            (mock/request :get "/"))]
      (is (= status 200)))))

(deftest test-app-404
  (is (= (:status ((app {:db *db*}) (mock/request :get "/wrong-path")))
         404)))

(deftest test-patients []
  (testing "get patients"
    (let [_ (jdbc/execute! *db* (sql/format {:insert-into :patient
                                             :columns [:full_name :gender :birthday :address :insurance]
                                             :values [["Vasya" "male" "1966-01-03" "homeless" "1234567890123456"]
                                                      ["Ludmila" "female" "1986-01-05" "Moscow" "1234567890129456"]]}))
          {:keys [status body]} ((app {:db *db*})
                                 (mock/request :get "/api/v1/patients?page=1&per-page=2&sort=desc"))
          result (->> (json/read-str body :key-fn keyword)
                      :data
                      (map #(:attributes %))
                      (map #(dissoc % :id)))]
      (is (= 200 status))
      (is (= [{:full_name "Ludmila"
               :gender "female"
               :birthday "1986-01-05"
               :address "Moscow"
               :insurance "1234567890129456"}
              {:full_name "Vasya"
               :gender "male"
               :birthday "1966-01-03"
               :address "homeless"
               :insurance "1234567890123456"}] result)))))

(deftest test-create-patient
  (testing "create patient"
    (let [data {:full_name "Vasya"
                :gender "male"
                :birthday "1945-11-19"
                :address "homeless"
                :insurance "1234567890123456"}
          {:keys [status body]} ((app {:db *db*})
                                 (-> (mock/request :post "/api/v1/patients")
                                     (mock/json-body {:data {:attributes data}})))
          {:keys [attributes]} (:data (json/read-str body :key-fn keyword))]

      (is (= 201 status))
      (is (= data attributes)))))

(deftest test-patient-delete
  (testing "delete patient"
    (let [{:keys [id]} (jdbc/execute-one!
                        *db*
                        (sql/format {:insert-into :patient
                                     :columns [:full_name :gender :birthday :address :insurance]
                                     :values [["Vasya" "male" "1966-01-03" "homeless" "1234567890123456"]]})
                        db-options)
          {:keys [status]} ((app {:db *db*})
                            (mock/request :delete (str "/api/v1/patients/" id)))]
      (is (= 204 status))
      (is (nil? (jdbc/execute-one!
                 *db*
                 (sql/format {:select [:*]
                              :from [:patient]
                              :where [:= :id id]})
                 db-options))))))

(deftest test-patient-update
  (testing "update patient"
    (let [{:keys [id]} (jdbc/execute-one!
                        *db*
                        (sql/format {:insert-into :patient
                                     :columns [:full_name :gender :birthday :address :insurance]
                                     :values [["Vasya" "male" "1966-01-03" "homeless" "1234567890123456"]]})
                        db-options)
          expected {:full_name "Maria"
                    :gender "female"
                    :birthday "1996-02-04"
                    :address "Moscow"
                    :insurance "6543210987654321"}
          {:keys [status]} ((app {:db *db*})
                            (-> (mock/request :patch (str "/api/v1/patients/" id))
                                (mock/json-body {:data {:attributes expected}})))
          result (-> (jdbc/execute-one!
                      *db*
                      (sql/format {:select [:full_name :gender :birthday :address :insurance]
                                   :from [:patient]
                                   :where [:= :id id]})
                      db-options)
                     (update :birthday unparse-date))]
      (is (= 200 status))
      (is (= result expected)))))
