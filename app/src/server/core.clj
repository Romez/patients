(ns server.core
  (:require
   [compojure.route :as route]
   [hiccup.page :refer [html5 include-js include-css]]
   [environ.core :refer [env]]
   [clj-time.jdbc]
   [clj-time.format :as format]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.util.response :refer [response status]])
  (:use compojure.core
        [server.utils :only (unparse-date)]
        [korma.db :only (defdb postgres)]
        [korma.core :only (defentity select insert values)]))

(defn page []
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/css/bootstrap.min.css")
    (include-css "/css/site.css")]
   [:body
    [:div#app "loading..."]
    (include-js "/js/app.js")]))

(defdb db (postgres {:host (:db-host env)
                     :port (:db-port env)
                     :db (:db-name env)
                     :user (:db-user env)
                     :password (:db-password env)
                     :stringtype "unspecified"}))

(defentity patient)

(defroutes handler
  (GET "/" [] (page))
  (context "/api/v1/patients" []
           (GET "/" [] (let [result
                             {:data (map (fn [record] {:id (:id record)
                                                  :attributes (-> record
                                                                  (dissoc :id)
                                                                  (update :birthday unparse-date))})
                                                (select patient))
                                     }]
                         (response result)))

           (POST "/" request (let [{:keys [body]} request
                                   record (insert patient (values (:attributes (:data body))))]
                               (-> (response {:data {:id (:id record)
                                                     :attributes (-> record
                                                                     (dissoc :id)
                                                                     (update :birthday unparse-date)
                                                                     )}})
                                   (status 201)))))
  (route/resources "/")
  (route/not-found "not found"))

(def app (-> handler
             wrap-json-response
             (wrap-json-body {:keywords? true :bigdecimals? true})))
