(ns patients.server
  (:require
   [compojure.route :as route]
   [compojure.core :refer [defroutes GET POST PATCH DELETE context]]
   [hiccup.page :refer [html5 include-js include-css]]
   [environ.core :refer [env]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.util.response :refer [response status]]
   [sentry-clj.core :as sentry]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [next.jdbc.date-time]
   [honeysql.core :as sql]
   [patients.validation :refer [validate-patient]]
   [patients.utils :refer [unparse-date]]))

(sentry/init! (:sentry-dsn env ""))

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

(defroutes handler
  (GET "/" [] (page))
  (context "/api/v1/patients" []
    (GET "/" request (let [{:keys [db params]} request
                           per-page (Integer/parseInt (:per-page params "10"))
                           current-page (Integer/parseInt (:page params "1"))
                           sort (keyword (:sort params "desc"))
                           {total :count} (jdbc/execute-one! db (sql/format {:select [:%count.*] :from [:patient]}))
                           last-page (-> total (/ per-page) Math/ceil int)
                           offset (* per-page (dec current-page))
                           records (jdbc/execute! db
                                                  (sql/format {:select [:*]
                                                               :from [:patient]
                                                               :limit per-page
                                                               :offset offset
                                                               :order-by [[:id sort]]})
                                                  {:builder-fn rs/as-unqualified-maps})
                           data (map (fn [r] {:id (:id r)
                                              :attributes (update r :birthday unparse-date)}) records)]
                       (response {:meta {:page {:current-page current-page
                                                :per-page per-page
                                                :last-page last-page
                                                :total total}}
                                  :data data})))
    (POST "/" request (let [{:keys [db body]} request
                            [errors data] (validate-patient (-> body :data :attributes))]
                        (if (not (nil? errors))
                          (-> (response {:errors errors})
                              (status 422))
                          (let [record (jdbc/execute-one!
                                        db
                                        (sql/format {:insert-into :patient
                                                     :columns (keys data)
                                                     :values [(vals data)]})
                                        {:builder-fn rs/as-unqualified-maps :return-keys true})
                                data {:data {:id (:id record)
                                             :attributes (-> record
                                                             (dissoc :id)
                                                             (update :birthday unparse-date))}}]
                            (-> (response data)
                                (status 201))))))
    (DELETE "/:id" {:keys [db params]}
      (let [result (jdbc/execute-one!
                    db
                    (sql/format {:delete-from :patient
                                 :where [:= :id (:id params)]})
                    {:builder-fn rs/as-unqualified-maps
                     :return-keys true})]
        (if (nil? result)
          {:status 404}
          {:status 204})))
    (PATCH "/:id" {:keys [db params body]}
      (let [[errors data] (validate-patient (-> body :data :attributes))]
        (if (not (nil? errors))
          (-> (response {:errors errors})
              (status 422))
          (let [result (jdbc/execute-one!
                        db
                        (sql/format {:update :patient
                                     :set data
                                     :where [:= :id (:id params)]})
                        {:builder-fn rs/as-unqualified-maps
                         :return-keys true})]
            (if (nil? result)
              {:status 404}
              {:status 200}))))))
  (route/resources "/")
  (route/not-found "not found"))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (if (:production env)
             (do
               (sentry/send-event {:throwable e})
               (-> (response "server error") (status 500)))
             (throw e))))))

(defn wrap-db [handler db]
  (fn [request]
    (handler (assoc request :db db))))

(defn app [{db :db}] (-> handler
                         (wrap-db db)
                         wrap-exception
                         (wrap-json-body {:keywords? true :bigdecimals? true})
                         wrap-json-response
                         wrap-keyword-params
                         wrap-params))

(def init (app {:db (jdbc/get-datasource
                      {:dbtype "postgresql"
                      :dbname (:db-name env)
                      :host (:db-host env)
                      :port (:db-port env)
                      :user (:db-user env)
                      :password (:db-password env)
                      :stringtype "unspecified"})}))
