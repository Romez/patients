(ns app.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [app.header :refer (header)]))

;; -------------------------
;; Views

(defn home-page []
  [:div {:class "container"}
   (header)])
;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
