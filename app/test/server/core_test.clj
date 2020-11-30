(ns server.core-test
  (:require [ring.mock.request :as mock]
            [korma.db :as db]
            [clj-time.jdbc]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clojure.data.json :as json])
  (:use [server.core :only (app patient)]
        [server.migration :only (migrate)]
        clojure.test
        [korma.core :only (select insert values)]))

(defn with-rollback
  [fn]
  (db/transaction
   (fn)
   (db/rollback)))

(defn fix-migrate
  [fn]
  (migrate)
  (fn))

(use-fixtures :once fix-migrate)
(use-fixtures :each with-rollback)

(deftest test-app-200
  (is (= (:status (app (mock/request :get "/")))
         200)))

(deftest test-app-402
  (is (= (:status (app (mock/request :get "/wrong-path")))
         404)))

(deftest test-patients
  (testing "get patients"
    (let [patients [{:full_name "Vasya"
                     :gender "male"
                     :birthday (time/date-time 1966 1 3)
                     :address "homeless"
                     :oms 456234456}
                    {:full_name "Ludmila"
                     :gender "female"
                     :birthday (time/date-time 1986 1 5)
                     :address "Moscow"
                     :oms 53242345}]
          _ (insert patient (values patients))
          {:keys [status body]} (app (mock/request :get "/api/v1/patients"))
          result (->> (json/read-str body :key-fn keyword)
                      :data
                      (map #(:attributes %))
                      (map #(update % :birthday format/parse)))]

      (is (= 200 status))
      (is (= patients result)))))

(deftest test-patient-create
  (testing "create patient"
    (let [data {:full_name "Vasya"
                                    :gender "male"
                                    :birthday "1945-11-19"
                                    :address "homeless"
                                    :oms 456234456}
          {:keys [status body]} (app  (-> (mock/request :post "/api/v1/patients")
                                          (mock/json-body {:data {:attributes data}})))
          result (-> (json/read-str body :key-fn keyword)
                     (:data)
                     (:attributes)
                     (update :birthday format/parse))
          expected (update data :birthday format/parse)]

      (is (= 201 status))
      (is (= expected result))
      (is (= expected (-> (select patient) first (dissoc :id)))))))
