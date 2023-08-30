(ns jeroenvandijk.edntail.impl.query
  (:require
   [edamame.core :as edamame]
   [jeroenvandijk.edntail.impl.incanter.core :refer [query-to-pred]]))


(defn parse-time-in-words [args]
  (letfn [(parse-next-time-arg [left-args]
            (if (or (next left-args)
                    (not (first left-args)))
              (throw (ex-info "Exactly one element expected after :since"))
              (let [arg (first left-args)]
                (case arg
                  #_:today #_(.. (java.time.LocalDate/now)
                             )
                  ; :yesterday

                  (cond
                    (number? arg) arg
                    (instance? java.util.Date arg) (.getTime arg)

                    :else
                    (throw (ex-info "Unexpected data format" {:el arg})))))))]
    (loop [sum 0
           [current & left-args] args
           quantity nil
           unit nil]
      (if-not current
        sum
        (cond
          (number? current)
          (recur sum left-args current nil)

          (or (symbol? current)
              (keyword? current)
              (string? current))
          (case (name current)
            ("second" "seconds")
            (recur (+ sum (* quantity 1000)) left-args nil nil)

            ("minute" "minutes")
            (recur (+ sum (* quantity 60 1000)) left-args nil nil)

            ("hour" "hours")
            (recur (+ sum (* quantity 60 60 1000)) left-args nil nil)

            ("day" "days")
            (recur (+ sum (* quantity 24 60 60 1000)) left-args nil nil)

            "ago"
            (if (first left-args)
              (throw (ex-info "No extra elements expected after :ago"))
              (do
                (println "Searching for "  (- (.getTime (java.util.Date.)) sum))
                (- (.getTime (java.util.Date.)) sum)))

            "before"
            (let [ts (- sum (parse-next-time-arg left-args))]
              (println "Searching for " (java.util.Date. ts))
              ts)

            "since"
            (let [ts (+ sum (parse-next-time-arg left-args))]
              (println "Searching for " (java.util.Date. ts))
              ts))

          :else
          (throw (ex-info "unexpected element" {:el current})))))))


(defn parse-query [query]
  (edamame/parse-string query {:readers {'time parse-time-in-words}}))


(defn query-transform [query]
  (let [{:keys [select where]} (parse-query query)]
    (cond-> identity
      where
      (comp (filter (query-to-pred where)))

      (and select
           (not (contains? (set select) '*)))
      (comp (map (fn [row]
                   (select-keys row select)))))))
