(ns patients.pages.home
  (:require
   [hiccup.page :refer [html5 include-js include-css]]))

(defn home-page []
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
