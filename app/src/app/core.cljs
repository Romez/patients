(ns app.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [ajax.core :refer [GET POST DELETE PATCH]]
      [clojure.string :refer [join split]]
      [reagent-forms.core :refer [bind-fields]]
      [app.header :refer [Header]]
      [app.routes :refer [get-patients-path get-patient-path]]
      [reagent-modals.modals :as reagent-modals]))

(def store (r/atom {:patients {}}))

(defn fetch-patients [handle-success handle-error]
  (GET (get-patients-path) {:handler handle-success
                            :error-handler handle-error
                            :response-format :json
                            :keywords? true}))

(defn handle-create [form-state]
  (fn [e]
    (.preventDefault e)
    (let [patient (update @form-state :birthday #(join "-" (vec (vals %))))
          data {:data {:attributes patient}}]
      (POST (get-patients-path) {:format :json
                                 :params data
                                 :handler (fn [{{id :id attributes :attributes} :data}]
                                            (reagent-modals/close-modal!)
                                            (swap! store update :patients #(assoc % id attributes)))
                                 :error-handler (fn [error] (js/console.log "error" error))
                                 :response-format :json
                                 :keywords? true}))))

(defn handle-delete [id]
  (fn [e]
    (.preventDefault e)
    (DELETE (get-patient-path id) {:handler (fn []
                                              (reagent-modals/close-modal!)
                                              (swap! store update :patients #(dissoc % id)))})))

(defn handle-update [id form-state]
  (fn [e]
    (.preventDefault e)
    (let [patient (update @form-state :birthday #(->> % vals vec (join "-")))
          data {:data {:attributes patient}}]
      (PATCH (get-patient-path id) {:format :json
                                    :params data
                                    :handler (fn []
                                               (reagent-modals/close-modal!)
                                               (swap! store update :patients #(assoc % id patient)))
                                    :error-handler (fn [error] (js/console.log "error" (str error)))}))))

(defn add-form []
  (let [form-state (r/atom {:full_name ""
                            :gender nil
                            :birthday {:year 1989 :month 1 :day 1}
                            :address ""
                            :oms nil})]
    [:form {:on-submit (handle-create form-state)}
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

(defn delete-form [id]
  [:form {:on-submit (handle-delete id)}
   [:div.modal-header [:h5.modal-title "Delete patient"]]
   [:div.modal-body
    [:p.h2 "Are you sure?"]
    [:div.d-flex.justify-content-between
     [:button.btn.btn-secondary.mr-2 {:type "button"
                                 :on-click #(reagent-modals/close-modal!)} "Close"]
     [:button.btn.btn-danger {:type "submit"} "Delete"]]]])

(defn update-form [id]
  (let [patient (update (get-in @store [:patients id]) :birthday #(zipmap [:year :month :day] (map int (split % #"-"))))
        form-state (r/atom patient)]
    [:form {:on-submit (handle-update id form-state)}
     [:div.modal-header [:h5.modal-title "Update patient"]]
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
      [:button.btn.btn-secondary {:type "button" :on-click #(reagent-modals/close-modal!)} "Close"]
      [:button.btn.btn-success {:type "submit"} "Update"]]]
    ))

(defn Patients [patients]
  [:ul.list-group
   (for [[id attributes] patients]
     ^{:key id} [:li.list-group-item.d-flex.justify-content-between.align-items-center
                            (:full_name attributes)
                            [:div
                             [:button.btn.btn-sm.btn-outline-success.fas.fa-edit.rounded-circle
                              {:on-click #(reagent-modals/modal! [update-form id])}]
                             [:button.btn.btn-sm.btn-outline-danger.fas.fa-trash-alt.ml-2.rounded-circle
                              {:on-click #(reagent-modals/modal! [delete-form id])}]]]
     )])

(defn App []
  [:div {:class "container"}
   (Header)
   [:button.btn.btn-success.mb-2 {:on-click #(reagent-modals/modal! [add-form])} "Add patient"]
   [Patients (:patients @store)]
   [reagent-modals/modal-window]])

(defn init! []
  (fetch-patients
   (fn [response]
     (let [patients (reduce #(assoc % (:id %2) (:attributes %2)) {} (:data response))]
       (swap! store assoc :patients patients)))

   #(js/console.log "ERROR"))
  (d/render [App] (.getElementById js/document "app")))
