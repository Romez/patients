(ns server.core
  (:require
   [compojure.route :as route]
   [hiccup.page :refer [html5 include-js include-css]])
  (:use compojure.core))

(defn page []
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (include-css "/css/site.css")]
   [:body
    [:div#app "app"]
    (include-js "/js/app.js")]))

(defroutes handler
  (GET "/" [] (page))
  (route/resources "/"))

(def run handler)
