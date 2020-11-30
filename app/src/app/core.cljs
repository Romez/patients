(ns app.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [reagent-forms.core :refer [bind-fields]]
      [app.header :refer [Header]]
      [ajax.core :refer [GET POST DELETE]]
      [app.routes :refer [get-patients-path get-delete-patient-path]]
      [clojure.string :refer [join]]
      [reagent-modals.modals :as reagent-modals]))

(def store (r/atom {:form {}
                    :patients []}))

(defn handle-submit [form-state]
  (fn [e]
    (.preventDefault e)

    (let [data {:data {:attributes (update @form-state :birthday #(join "-" (vec (vals %))) )}}]
      (POST (get-patients-path) {:format :json
                                 :params data
                                 :handler (fn [response]
                                            (reagent-modals/close-modal!)
                                            ;; TODO add to store
                                            )
                                 :error-handler (fn [error] (js/console.log "error" error))
                                 :response-format :json
                                 :keywords? true}))))

(defn handle-delete [id]
  (fn [e]
    (.preventDefault e)
    (DELETE (get-delete-patient-path id) {:handler (fn []
                                                     (reagent-modals/close-modal!)
                                                     ;; TODO remove from store
                                                     )})))

(defn add-form []
  (let [form-state (r/atom {:full_name ""
                              :gender nil
                              :birthday {:year 1989 :month 1 :day 1}
                              :address ""
                            :oms nil})]
    [:form {:on-submit (handle-submit form-state)}
     [:div.modal-header [:div.h5.modal-title "Add patient"]]
      [:div.modal-body
       [bind-fields
        [:<>
         [:div.form-group [:label "Full name" [:input.form-control {:field :text :id :full_name}]]]
         [:div.form-group
          [:div.form-check [:label [:input.form-check-input {:field :radio :value :male :name :gender}] "Male" ]]
          [:div.form-check [:label [:input.form-check-input {:field :radio :value :female :name :gender}] "Female"]]]
         [:div.form-group [:label "Birthday" [:div {:field :datepicker :id :birthday }]]]
         [:div.form-group [:label "Address" [:input.form-control {:field :text :id :address}]]]
         [:div.form-group [:label "OMS" [:input.form-control {:field :numeric :id :oms}]]]]
        form-state]]
      [:div.modal-footer
       [:button.btn.btn-secondary {:type "button"
                                   :on-click #(reagent-modals/close-modal!)} "Close"]
       [:button.btn.btn-success {:type "submit"} "Submit"]]]))

(defn delte-form [id]
  [:form {:on-submit (handle-delete id)}
   [:div.modal-header [:h5.modal-title "Delete patient"]]
   [:div.modal-body
    [:p.h2 "Are you sure?"]
    [:div.d-flex.justify-content-between
     [:button.btn.btn-secondary.mr-2 {:type "button"
                                 :on-click #(reagent-modals/close-modal!)} "Close"]
     [:button.btn.btn-danger {:type "submit"} "Delete"]]]])


(defn Patients [patients]
  [:ul.list-group
   (for [patient patients]
     ^{:key (:id patient)} [:li.list-group-item
                            (:full_name (:attributes patient))
                            [:button.btn.btn-sm.btn-outline-danger.fas.fa-trash-alt.ml-2.rounded-circle
                             {:on-click #(reagent-modals/modal! [delte-form (:id patient)])}]])])

(defn App []
  [:div {:class "container"}
   (Header)
   [:button.btn.btn-success.mb-2 {:on-click #(reagent-modals/modal! [add-form])} "Add patient"]
   [Patients (:patients @store)]
   [reagent-modals/modal-window]])

(defn init! []
  (GET (get-patients-path) {:handler (fn [response]
                                         (swap! store assoc :patients (:data response)))
                              :error-handler #(js/console.log "ERROR")
                              :response-format :json
                              :keywords? true})

    (d/render [App] (.getElementById js/document "app")))
