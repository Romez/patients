(ns patients.validation
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
            [patients.i18n :refer [tr]]
            #?(:clj [clj-time.format :refer [formatters]])
            #?(:cljs [cljs-time.format :refer [ formatters]])))

(def patient-schema
  {:full_name [[v/required :message (tr [:validation.patient.full-name/required "Required"])]
               v/string]
   :gender    [[v/required :message (tr [:validation.patient.gender/required "Required"])]
               [v/member #{"male" "female"} :message (tr [:validation.patient.gender/wrong-value "Wrong value"])]]
   :birthday  [[v/required :message (tr [:validation.patient.birthday/required "Required"])]
               v/string
              [v/datetime (:date formatters)]]
   :address   [v/string]
   :insurance [[v/required :message (tr [:validation.patient.insurance/required])]
               v/string
               [v/matches #"^\d{16}$" :message (tr [:validation.patient.insurance/matches])]]})

(defn validate-patient [patient] (b/validate patient patient-schema))
