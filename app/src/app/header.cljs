(ns app.header)

(defn Header []
  [:nav.mb-4 {:class "navbar navbar-expand-lg navbar-light bg-light"}
   [:a {:class "navbar-brand" :href "#"} "Patients"]])
