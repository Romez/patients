(ns patients.validation
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
            [patients.i18n :refer [tr]]
            #?(:clj [clj-time.format :refer [formatters]])
            #?(:cljs [cljs-time.format :refer [ formatters]])))

(def patient-schema {:full_name [v/required v/string]
              :gender [[v/required :message (tr [:validation.patient.gender/required])]
                       [v/member #{"male" "female"} :message (tr [:validation.patient.gender/wrong-value])]]
              :birthday [v/required v/string [v/datetime (:date formatters)]]
              :address [v/string]
              [:insurance] [v/required v/string [v/matches #"\d{16}"]]})

(defn validate-patient [patient] (b/validate patient patient-schema))
