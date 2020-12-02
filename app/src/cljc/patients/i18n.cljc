(ns patients.i18n
  (:require [taoensso.tempura :as tempura]))

(def en {:brand "Patients"
         :create "Create patient"
         :close "Close"
         :modals {:create {:title "Create patient"
                           :submit "Create"}
                  :update {:title "Update patient"
                           :submit "Update"}
                  :delete {:title "Delete patient"
                           :submit "Delete"
                           :question "Are you sure?"}}
         :patient {:full-name "Full name"
                   :gender {:male "Male" :female "Female"}
                   :birthday "Birthday"
                   :address "Address"
                   :insurance "Insurance number"}})

(def tr (partial tempura/tr {:dict {:en en}} [:en]))
