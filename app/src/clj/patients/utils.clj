(ns patients.utils
  (:require [clj-time.format :as format]))

(defn unparse-date [date] (format/unparse (format/formatters :date) date))
