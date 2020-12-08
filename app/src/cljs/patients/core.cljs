(ns patients.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [clojure.set :refer [intersection]]
   [ajax.core :refer [GET POST DELETE PATCH]]
   [clojure.string :refer [join]]
   [reagent-modals.modals :as reagent-modals]
   [secretary.core :as secretary]
   [accountant.core :as accountant]
   [fork.reagent :as fork]
   [patients.routes :refer [get-patients-path get-patient-path]]
   [patients.validation :refer [patient-schema]]
   [patients.i18n :refer [tr]]
   [bouncer.core :as b]))

(def store (r/atom {:fetching-patients {:status :idle :error nil}
                    :patients {:byId {} :allIds []}
                    :query-params {}
                    :page {:last-page 1 :total 0}}))

(defn fetch-patients []
  (let [qp (select-keys (:query-params @store ) [:page :per-page])]
    (swap! store update :fetching-patients #(assoc % :status :loading))

    (GET (get-patients-path qp)
         {:response-format :json
          :keywords? true
          :handler (fn [{:keys [data meta]}]
                     (let [patients (reduce (fn [{allIds :allIds byId :byId} item]
                                              {:byId (assoc byId (:id item) (:attributes item))
                                               :allIds (conj allIds (:id item))})
                                            {:byId {} :allIds []}
                                            data)
                           {:keys [last-page total]} (:page meta)]
                       (swap! store assoc
                              :patients patients
                              :page {:last-page (js/parseInt last-page)
                                     :total (js/parseInt total)}
                              :fetching-patients {:status :success :error nil})))
          :error-handler (fn [error]
                           (swap! store assoc :fetching-patients {:status :failure :error (:status-text error)})
                           (throw (str error)))})))

#_:clj-kondo/ignore
(defroute home-path "/" [_ query-params]
  (swap! store assoc :query-params query-params)
  (fetch-patients))

(defn delete-form [id]
  [fork/form {:path :delete-patient
              :prevent-default? true
              :on-submit (fn [{state :state path :path}]
                           (swap! state fork/set-submitting path true)
                           (DELETE (get-patient-path id)
                                   {:finally (fn []
                                               (swap! state fork/set-submitting path false))
                                    :handler (fn []
                                               (reagent-modals/close-modal!)
                                               (fetch-patients))
                                    :error-handler (fn [error]
                                                     (swap! state fork/set-server-message path (:status-text error))
                                                     (throw (str error)))}))}
   (fn [{:keys [handle-submit
                submitting?
                on-submit-server-message]}]
     [:form {:on-submit handle-submit}
      [:div.modal-header [:h5.modal-title (tr [:modals.delete/title])]]
      [:div.modal-body
       [:p.h2 (tr [:modals.delete/question])]

       [:p.text-danger on-submit-server-message]

       [:div.d-flex.justify-content-between
        [:button.btn.btn-secondary.mr-2
         {:type "button"
          :on-click #(reagent-modals/close-modal!)}
         (tr [:close])]
        [:button.btn.btn-danger
         {:type "submit" :disabled submitting?}
         (if submitting?
           [:<>
            [:span.spinner-border.spinner-border-sm.mr-2]
            (tr [:loading])]
           (tr [:modals.delete/submit]))]]]])])

(defn patient-form [title initial-values handle-submit]
  (let [state (r/atom {})
        path :form]
    [fork/form {:path path
                :state state
                :keywordize-keys true
                :prevent-default? true
                :validation (fn [data]
                              (let [[errors] (b/validate data (select-keys patient-schema (vec (keys data))))
                                    client-errors (reduce (fn [acc [k v]] (assoc acc k (join ", " v))) {} errors)
                                    server-errors (get-in @state [path :server])]

                                (doseq [key (intersection (set (keys data)) (set (keys server-errors)))]
                                  (swap! state update-in [:form :server] #(dissoc % key)))

                                client-errors))
                :initial-values initial-values
                :on-submit handle-submit}
      (fn [{:keys [values
                   handle-change
                   errors
                   server-errors
                   submitting?
                   handle-submit
                   on-submit-server-message]}]
        [:form {:id :patient-form
                :noValidate true
                :class (when (not-empty errors) "was-invalidated")
                :on-submit handle-submit}
          [:div.modal-header [:h5.modal-title title]]
          [:div.modal-body
          [:div.form-group
            [:label {:for :full_name} (tr [:patient/full-name])]
            [:input.form-control
            {:id :full_name
              :class (when (not (nil? (or (:full_name server-errors)
                                          (:full_name errors)))) "is-invalid")
              :name :full_name
              :value (values :full_name)
              :on-change handle-change}]
            [:div.invalid-feedback (or (:full_name errors)
                                      (:full_name server-errors))]]
          [:div.form-group
            [:div.form-check
            [:input.form-check-input {:type :radio
                                      :required true
                                      :name :gender
                                      :class (when (not (nil? (or (:gender server-errors)
                                                                  (:gender errors)))) "is-invalid")
                                      :checked (= "male" (values :gender))
                                      :value "male"
                                      :on-change handle-change
                                      :id :male}]
            [:label.form-check-label {:for :male} (tr [:patient.genders/male])]]
            [:div.form-check
            [:input.form-check-input {:type :radio
                                      :required true
                                      :value "female"
                                      :class (when (not (nil? (or (:gender server-errors)
                                                                  (:gender errors)))) "is-invalid")
                                      :checked (= "female" (values :gender))
                                      :on-change handle-change
                                      :name :gender
                                      :id :female}]
            [:label.form-check-label {:for :female} (tr [:patient.genders/female])]
            [:div.invalid-feedback (or (:gender server-errors)
                                        (:gender errors))]]]
          [:div.form-group
            [:label {:for :birthday} (tr [:patient/birthday])]
            [:input.form-control {:type :date
                                  :class (when (not (nil? (or (:birthday server-errors)
                                                              (:birthday errors)))) "is-invalid")
                                  :name :birthday
                                  :id :birthday
                                  :value (values :birthday)
                                  :on-change handle-change}]
           [:div.invalid-feedback (or (:birthday server-errors)
                                      (:birthday errors))]]
          [:div.form-group
            [:label {:for :address} (tr [:patient/address])]
            [:input.form-control {:id :address
                                  :name :address
                                  :class (when (not (nil? (or (:address server-errors)
                                                              (:address errors)))) "is-invalid")
                                  :value (values :address)
                                  :on-change handle-change}]
            [:div.invalid-feedback (or (:address server-errors)
                                      (:address errors))]]
          [:div.form-group
            [:label {:for :insurance} (tr [:patient/insurance])]
            [:input.form-control {:id :insurance
                                  :name :insurance
                                  :class (when (not (nil? (or (:insurance server-errors)
                                                              (:insurance errors)))) "is-invalid")
                                  :value (values :insurance)
                                  :on-change handle-change}]
           [:div.invalid-feedback (or (:insurance server-errors)
                                      (:insurance errors))]]]
         [:p.text-danger on-submit-server-message]
          [:div.modal-footer
            [:button.btn.btn-secondary
              {:type "button" :on-click #(reagent-modals/close-modal!)}
              (tr [:close])]
            [:button.btn.btn-success
              {:type "submit" :disabled submitting?}
              (if submitting?
                [:<>
                [:span.spinner-border.spinner-border-sm.mr-2]
                (tr [:loading])]
                (tr [:submit]))]]])]))

(defn create-form []
  (patient-form
   (tr [:modals.create/title])
   {}
   (fn [{path :path state :state values :values}]
     (swap! state fork/set-submitting path true)
     (POST (get-patients-path {})
           {:format :json
            :response-format :json
            :keywords? true
            :params {:data {:attributes values}}
            :finally #(swap! state fork/set-submitting path false)
            :handler (fn []
                       (fetch-patients)
                       (reagent-modals/close-modal!))
            :error-handler (fn [{:keys [status response] :as error}]
                             (if (= status 422)
                               (doseq [[k v] (:errors response)]
                                 (swap! state fork/set-error path k (join ", " v)))
                               (do
                                 (swap! state fork/set-server-message path (:status-text error))
                                 (throw error))))}))))

(defn update-form [id]
  (patient-form
   (tr [:modals.update/title])
   (get-in @store [:patients :byId id])
   (fn [{path :path state :state values :values}]
     (swap! state fork/set-submitting path true)
     (PATCH (get-patient-path id)
      {:format :json
        :keywords? true
        :params {:data {:attributes values}}
        :finally #(swap! state fork/set-submitting path false)
        :handler (fn []
                   (swap! store assoc-in [:patients :byId id] values)
                   (reagent-modals/close-modal!))
        :error-handler (fn [{:keys [status response] :as error}]
                        (if (= status 422)
                          (doseq [[k v] (:errors response)]
                            (swap! state fork/set-error path k (join ", " v)))
                          (do
                            (swap! state fork/set-server-message path (:status-text error))
                            (throw error))))}))))

(defn view [id]
  (let [patient (get-in @store [:patients :byId id])]
    [:<>
     [:div.modal-header [:h5.modal-title (tr [:modals.view/title])]]
     [:div.modal-body
      [:ul.list-group.list-group-flush
       [:li.list-group-item [:strong (tr [:patient/full-name])": "] (:full_name patient)]
       [:li.list-group-item [:strong (tr [:patient/gender])": "] (:gender patient)]
       [:li.list-group-item [:strong (tr [:patient/birthday])": "] (:birthday patient)]
       [:li.list-group-item [:strong (tr [:patient/address])": "] (:address patient)]
       [:li.list-group-item [:strong (tr [:patient/insurance])": "] (:insurance patient)]]
      [:button.btn.btn-secondary
       {:type "button" :on-click #(reagent-modals/close-modal!)}
       (tr [:close])]]]))

(defn Patients [patients total fetching-patients]
  [:div.position-relative
   (when (= (:status fetching-patients) :failure )
     [:p.text-danger (:error fetching-patients)])

   (when (= (:status fetching-patients) :loading )
     [:div.d-flex.justify-content-center.align-items-center.w-100.h-100.position-absolute
      {:style {:left 0 :top 0}}
      [:div.spinner-border.text-primary
       [:span.sr-only (tr [:loading])]]])
   [:table.table
    [:caption (tr [:total] [total])]
    [:thead
     [:tr
      [:th (tr [:patient/full-name])]
      [:th (tr [:patient/insurance])]
      [:th (tr [:actions])]]]

    [:tbody
     (doall
      (map (fn [{:keys [id full_name insurance]}]
             ^{:key id} [:tr
                         [:td full_name]
                         [:td insurance]
                         [:td
                          [:button.btn.btn-sm.btn-outline-info.fas.fa-eye.rounded-circle.mr-2
                           {:on-click #(reagent-modals/modal! [view id])}]
                          [:button.btn.btn-sm.btn-outline-success.fas.fa-edit.rounded-circle.mr-2
                           {:on-click #(reagent-modals/modal! [update-form id])}]
                          [:button.btn.btn-sm.btn-outline-danger.fas.fa-trash-alt.rounded-circle
                           {:on-click #(reagent-modals/modal! [delete-form id])}]]]
             ) patients))]]])

(defn Pagination [last-page current-page qp]
  [:nav
   [:ul.pagination
    [:li.page-item
     {:class (when (= current-page 1) "disabled")}
       [:a.page-link
        {:href (home-path {:query-params (assoc qp :page (dec current-page))})}
        "<<"]]
    (for [page (range 1 (inc last-page))]
      ^{:key page}
        [:li.page-item
         {:class (when (= page current-page) "active")}
         [:a.page-link {:href (home-path {:query-params (assoc qp :page page)})}
          page]])
    [:li.page-item
       {:class (when (= current-page last-page) "disabled")}
       [:a.page-link
        {:href (home-path {:query-params (assoc qp :page (inc current-page))})}
        ">>"]]]])

(defn App []
  [:div {:class "container"}
     [:nav.mb-4 {:class "navbar navbar-expand-lg navbar-light bg-light"}
      [:a {:class "navbar-brand" :href "#"} (tr [:brand])]]

     [:button.btn.btn-success.mb-2 {:on-click #(reagent-modals/modal! [create-form])} (tr [:create])]

     [Patients
      (map #(get-in @store [:patients :byId %]) (-> @store :patients :allIds))
      (-> @store :page :total)
      (:fetching-patients @store)]

     [Pagination
      (-> @store :page :last-page)
      (js/parseInt (or (-> @store :query-params :page) 1))
      (:query-params @store)]

     [reagent-modals/modal-window]])

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler (fn [path] (secretary/dispatch! path))
    :path-exists?  (fn [path] (secretary/locate-route path))})
  (accountant/dispatch-current!)

  (d/render [App] (.getElementById js/document "app")))
