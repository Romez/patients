(ns patients.server
  (:require
   [compojure.route :as route]
   [hiccup.page :refer [html5 include-js include-css]]
   [environ.core :refer [env]]
   [clj-time.jdbc]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
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
           (GET "/" request (let [per-page (Integer/parseInt (get-in request [:params :per-page] "10"))
                                  current-page (Integer/parseInt (get-in request [:params :page] "1"))
                                  sort (keyword (get-in request [:params :sort] "desc"))
                                  total (-> (korma/select patient (korma/aggregate (count :*) :count))
                                            first
                                            :count)
                                  last-page (-> total (/ per-page) Math/ceil int)
                                  data (->> (korma/select patient
                                                          (korma/order :id sort)
                                                          (korma/limit per-page)
                                                          (korma/offset (* per-page (dec current-page))))
                                            (map (fn [r] {:id (:id r)
                                                     :attributes (update r :birthday unparse-date)})))]
                         (response {:meta {:page {:last-page last-page
                                                  :current-page current-page
                                                  :per-page per-page
                                                  :total total}}
                                    :data data})))

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
             (wrap-json-body {:keywords? true :bigdecimals? true})
             wrap-json-response
             wrap-keyword-params
             wrap-params))
