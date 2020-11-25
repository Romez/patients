(ns app.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]))

;; -------------------------
;; Views

(defn foo [] (println "x"))

(defn home-page []
  [:div
   [:h1 "2 Patients"]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
