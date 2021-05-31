(ns patients.i18n
  (:require [taoensso.tempura :as tempura]))

(def en {:brand "Patients"
         :create "Create patient"
         :close "Close"
         :actions "Actions"
         :total "Total: %1"
         :loading "Loading"
         :submit "Submit"
         :modals {
                  :create {:title "Create patient"
                           :submit "create"}
                  :update {:title "Update patient"
                           :submit "Update"}
                  :delete {:title "Delete patient"
                           :submit "Delete"
                           :question "Are you sure?"}
                  :view {:title "View patient"}}
         :patient {:full-name "Full name"
                   :gender "Gender"
                   :genders {:male "Male" :female "Female"}
                   :birthday "Birthday"
                   :address "Address"
                   :insurance "Insurance number"}
         :validation {:patient {:gender {:required "Select gender"
                                         :wrong-value "Wrong value"}
                                :insurance {:required "Please fill insurance"
                                            :matches "Insurance number should be 16 numbers"}}}})

(def tr (partial tempura/tr {:dict {:en en}} [:en]))
