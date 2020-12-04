(ns patients.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [ajax.core :refer [GET POST DELETE PATCH]]
   [clojure.string :refer [join split]]
   [reagent-modals.modals :as reagent-modals]
   [patients.routes :refer [get-patients-path get-patient-path]]
   [patients.validation :as validation]
   [patients.i18n :refer [tr]]
   [secretary.core :as secretary]
   [accountant.core :as accountant]))

(def store (r/atom {:patients {:byId {} :allIds []}
                    :query-params {}
                    :page {:last-page 1
                           :total 0}}))

(defn fetch-patients []
  (let [qp (select-keys (:query-params @store ) [:page :per-page])]
    (GET (get-patients-path qp)
         {:handler (fn [{:keys [data meta]}]
                     (let [patients (reduce (fn [{allIds :allIds byId :byId} item]
                                              {:byId (assoc byId (:id item) (:attributes item))
                                               :allIds (conj allIds (:id item))})
                                            {:byId {} :allIds []}
                                            data)
                           {:keys [last-page total]} (:page meta)]
                       (swap! store assoc
                              :patients patients
                              :page {:last-page (js/parseInt last-page)
                                     :total (js/parseInt total)})))
          :error-handler (fn [error]
                           ;; TODO handle error
                           (js/console.error (str error)))
          :response-format :json
          :keywords? true})))

(defroute home-path "/" [_ query-params]
  (swap! store assoc :query-params query-params)
  (fetch-patients))

(defn handle-create [form-state errors]
  (fn [e]
    (.preventDefault e)
    (let [data {:data {:attributes @form-state}}]
      (POST (get-patients-path {})
            {:params data
             :handler (fn []
                        (reagent-modals/close-modal!)
                        (fetch-patients))
             :error-handler (fn [{:keys [status response] :as error}]
                              (if (= status 422)
                                (let [result (reduce
                                              (fn [acc [k v]] (assoc acc k (join ", " v)))
                                              {}
                                              (:errors response))]
                                  (reset! errors result))
                                (js/console.error error)))
             :format :json
             :response-format :json
             :keywords? true}))))

(defn handle-delete [id]
  (fn [e]
    (.preventDefault e)
    (DELETE (get-patient-path id) {:handler (fn []
                                              (reagent-modals/close-modal!)
                                              (fetch-patients))
                                   :error-handler (fn [error]
                                                    ;; TODO handle error
                                                    (js/console.error (str error)))})))

(defn handle-update [id form-state errors]
  (fn [e]
    (.preventDefault e)
    (let [data {:data {:attributes @form-state}}]
      (PATCH (get-patient-path id)
             {:format :json
              :params data
              :handler (fn []
                         (swap! store assoc-in [:patients :byId id] @form-state)
                         (reagent-modals/close-modal!))
              :error-handler (fn [{:keys [status response] :as error}]
                               (if (= status 422)
                                 (let [result (reduce
                                               (fn [acc [k v]] (assoc acc k (join ", " v)))
                                               {}
                                               (:errors response))]
                                   (reset! errors result))
                                 (js/console.error error)))}))))

(defn input [form-state errors key label]
  [:div.form-group
   [:label {:for key} label]
   [:input.form-control {:field :text
                         :class (when (not (nil? (key @errors))) "is-invalid")
                         :id key
                         :value (key @form-state)
                         :on-change #(swap! form-state assoc key (-> % .-target .-value))}]
   [:div.invalid-feedback (key @errors)]])

(defn radio [form-state errors name value label]
  [:div.form-check
   [:input.form-check-input {:type :radio
                             :required true
                             :class (when (not (nil? (name @errors))) "is-invalid")
                             :checked (= value (keyword (name @form-state)))
                             :on-change #(swap! form-state assoc name value)
                             :name name
                             :id value}]
   [:label.form-check-label {:for value} label]])

(defn datepicker [form-state errors key label]
  [:div.form-group
   [:label {:for :birthday} label]
   [:input.form-control {:type :date
                         :class (when (not (nil? (key @errors))) "is-invalid")
                         :id key
                         :value (key @form-state)
                         :on-change #(swap! form-state assoc key (-> % .-target .-value))}]
   [:div.invalid-feedback (key @errors)]])

(defn create-form []
  (let [form-state (r/atom {:full_name "" :gender nil :birthday "" :address "" :insurance ""})
        errors (r/atom {})]
    (fn []
      [:form.needs-validation {:noValidate true
                               :class (when (not (empty? @errors)) "was-invalidated")
                               :on-submit (handle-create form-state errors)}
       [:div.modal-header [:div.h5.modal-title (tr [:modals.create/title])]]
       [:div.modal-body
        [input form-state errors :full_name (tr [:patient/full-name])]
        [:div.form-group
         [radio form-state errors :gender :male (tr [:patient.genders/male])]
         [radio form-state errors :gender :female (tr [:patient.genders/female])]]
        [datepicker form-state errors :birthday (tr [:patient/birthday])]
        (input form-state errors :address (tr [:patient/address]))
        (input form-state errors :insurance (tr [:patient/insurance]))]
       [:div.modal-footer
        [:button.btn.btn-secondary
         {:type "button" :on-click #(reagent-modals/close-modal!)}
         (tr [:close])]
        [:button.btn.btn-success
         {:type "submit"}
         (tr [:modals.create/submit])]]])))

(defn update-form [id]
  (let [form-state (r/atom (get-in @store [:patients :byId id]))
        errors (r/atom {})]
    (fn []
      [:form {:noValidate true
              :class (when (not (empty? @errors)) "was-invalidated")
              :on-submit (handle-update id form-state errors)}
       [:div.modal-header [:h5.modal-title (tr [:modals.update/title])]]
       [:div.modal-body
        [input form-state errors :full_name (tr [:patient/full-name])]
        [:div.form-group
         [radio form-state errors :gender :male (tr [:patient.genders/male])]
         [radio form-state errors :gender :female (tr [:patient.genders/female])]]
        [datepicker form-state errors :birthday (tr [:patient/birthday])]
        (input form-state errors :address (tr [:patient/address]))
        (input form-state errors :insurance (tr [:patient/insurance]))]
       [:div.modal-footer
        [:button.btn.btn-secondary
         {:type "button" :on-click #(reagent-modals/close-modal!)}
         (tr [:close])]
        [:button.btn.btn-success
         {:type "submit"}
         (tr [:modals.update/submit])]]])))

(defn delete-form [id]
  [:form {:on-submit (handle-delete id)}
   [:div.modal-header [:h5.modal-title (tr [:modals.delete/title])]]
   [:div.modal-body
    [:p.h2 (tr [:modals.delete/question])]
    [:div.d-flex.justify-content-between
     [:button.btn.btn-secondary.mr-2 {:type "button" :on-click #(reagent-modals/close-modal!)} (tr [:close])]
     [:button.btn.btn-danger {:type "submit"} (tr [:modals.delete/submit])]]]])

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

(defn Patients [patients total]
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
          ) patients))]])

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
  (let [patients (map #(get-in @store [:patients :byId %]) (-> @store :patients :allIds))
        last-page (-> @store :page :last-page)
        current-page (-> @store :query-params :page (js/parseInt))
        total (-> @store :page :total)
        qp (:query-params @store)]
    [:div {:class "container"}
            [:nav.mb-4 {:class "navbar navbar-expand-lg navbar-light bg-light"}
             [:a {:class "navbar-brand" :href "#"} (tr [:brand])]]
            [:button.btn.btn-success.mb-2 {:on-click #(reagent-modals/modal! [create-form])} (tr [:create])]
            [Patients patients total]
            [Pagination last-page current-page qp]
            [reagent-modals/modal-window]]))

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler (fn [path] (secretary/dispatch! path))
    :path-exists?  (fn [path] (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (d/render [App] (.getElementById js/document "app")))
