(ns app.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [reagent-forms.core :refer [bind-fields]]
      [app.header :refer [header]]
      [ajax.core :refer [GET POST]]
      [app.routes :refer [get-patients-path]]
      [clojure.string :refer [join]]))

(def form-state
  (r/atom {:full_name ""
           :gender nil
           :birthday {:year 1989 :month 1 :day 1}
           :address ""
           :oms nil}))

(def patients (r/atom []))

(defn handle-submit [e]
  (.preventDefault e)

  (let [data {:data {:attributes (update @form-state :birthday #(join "-" (vec (vals %))) )}}]
    (POST (get-patients-path) {:format :json
                               :params data
                               :handler (fn [response] (js/console.log "success" response))
                               :error-handler (fn [error] (js/console.log "error" error))
                               :response-format :json
                               :keywords? true})))

(defn form []
  (fn []
      [bind-fields
       [:form {:on-submit handle-submit}
        [:div.form-group [:label "Full name" [:input.form-control {:field :text :id :full_name}]]]
        [:div.form-group
         [:div.form-check [:label [:input.form-check-input {:field :radio :value :male :name :gender}] "Male" ]]
         [:div.form-check [:label [:input.form-check-input {:field :radio :value :female :name :gender}] "Female"]]]
        [:div.form-group [:label "Birthday" [:div {:field :datepicker :id :birthday }]]]
        [:div.form-group [:label "Address" [:input.form-control {:field :text :id :address}]]]
        [:div.form-group [:label "OMS" [:input.form-control {:field :numeric :id :oms}]]]
        [:button.btn.btn-success {:type "submit"} "Submit"]]
       form-state]))

(defn home-page []
  (let []
    (fn []
      [:div {:class "container"}
       (header)
       [form]
       (:p @patients)
       [:ul
        (for [patient @patients]
          ^{:key (:id patient)} [:li (:full_name (:attributes patient))])]
       ])))

(defn init! []
  (GET (get-patients-path) {:handler (fn [response]
                                       (reset! patients (:data response)))
                            :error-handler #(js/console.log "ERROR")
                            :response-format :json
                            :keywords? true})
  (d/render [home-page] (.getElementById js/document "app")))
