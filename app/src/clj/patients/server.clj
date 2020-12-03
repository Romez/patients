(ns patients.server
  (:require
   [compojure.route :as route]
   [hiccup.page :refer [html5 include-js include-css]]
   [environ.core :refer [env]]
   [clj-time.jdbc]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.util.response :refer [response status]]
   [korma.core :as korma]
   [patients.validation :refer [validate-patient]]
   [clojure.spec.alpha :as s])
  (:use compojure.core
        [patients.utils :only (unparse-date)]
        [korma.db :only (defdb postgres)]))

(defn page []
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (include-css "https://use.fontawesome.com/releases/v5.7.0/css/all.css")
    (include-css "https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/css/bootstrap.min.css")
    (include-css "/css/site.css")]
   [:body
    [:div#app "loading..."]
    (include-js "https://code.jquery.com/jquery-3.2.1.slim.min.js")
    (include-js "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js")
    (include-js "/js/app.js")]))


(defdb db (postgres {:host (:db-host env)
                     :port (:db-port env)
                     :db (:db-name env)
                     :user (:db-user env)
                     :password (:db-password env)
                     :stringtype "unspecified"}))

(korma/defentity patient)

(defroutes handler
  (GET "/" [] (page))
  (context "/api/v1/patients" []
           (GET "/" [] (let [result
                             {:data (map (fn [record] {:id (:id record)
                                                  :attributes (-> record
                                                                  (dissoc :id)
                                                                  (update :birthday unparse-date))})
                                                (korma/select patient))}]
                         (response result)))

           (POST "/" request (let [{:keys [body]} request
                                   [errors data] (validate-patient (-> body :data :attributes))]
                               (if (not (nil? errors))
                                 (-> (response {:errors errors}) (status 422))
                                 (let [record (korma/insert patient (korma/values data))
                                       data {:data {:id (:id record)
                                                    :attributes (-> record (dissoc :id) (update :birthday unparse-date))}}]
                                   (-> (response data) (status 201))))))
           (DELETE "/:id" [id]
                   (if (> (korma/delete patient (korma/where {:id id})) 0)
                     {:status 204}
                     {:status 404}))
           (PATCH "/:id" {:keys [params body]}
                  (let [[errors data] (validate-patient (-> body :data :attributes))]
                    (if (not (nil? errors))
                       (-> (response {:errors errors}) (status 422))
                       (if (> (korma/update
                               patient
                               (korma/set-fields (-> body :data  :attributes))
                               (korma/where {:id (:id params)})) 0)
                         {:status 200}
                         {:status 404})))))
  (route/resources "/")
  (route/not-found "not found"))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           ;; TODO send to log
           {:status 500 :body "Exception caught"}))))

(def app (-> handler
             wrap-exception
             wrap-json-response
             (wrap-json-body {:keywords? true :bigdecimals? true})))
