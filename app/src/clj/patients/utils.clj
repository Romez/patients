(ns patients.utils)

(defn unparse-date [date] (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") date))
