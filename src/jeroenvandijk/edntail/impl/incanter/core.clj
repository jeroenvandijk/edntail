(ns jeroenvandijk.edntail.impl.incanter.core)

;; From https://github.com/incanter/incanter/blob/2b6a9ab05665114222ab55a8711df5787c8a9467/modules/incanter-core/src/incanter/core.clj#L1257
;; Licensed under Eclipse Public License - Version 1.0

(defn query-to-pred
  "
  Given a query-map, it returns a function that accepts a hash-map and returns true if it
  satisfies the conditions specified in the provided query-map.
  Examples:
    (use 'incanter.core)
    (def pred (query-to-pred {:x 5 :y 7}))
    (pred {:x 5 :y 7 :z :d})
    (def pred (query-to-pred {:x 5 :y {:$gt 5 :$lt 10}}))
    (pred {:x 5 :y 7 :z :d})
    (def pred (query-to-pred {:z {:$in #{:a :b}}}))
    (pred {:x 5 :y 7 :z :d})
  "
  ([query-map]
    (let [in-fn (fn [value val-set] (some val-set [value]))
          nin-fn (complement in-fn)
          ops {:gt #(> (compare %1 %2) 0)
               :lt #(< (compare %1 %2) 0)
               :eq =
               :ne not=
               :gte #(>= (compare %1 %2) 0)
               :lte #(<= (compare %1 %2) 0)
               :in in-fn :nin nin-fn :fn (fn [v f] (f v))
               :$gt #(> (compare %1 %2) 0)
               :$lt #(< (compare %1 %2) 0)
               :$eq = :$ne not=
               :$gte #(>= (compare %1 %2) 0)
               :$lte #(<= (compare %1 %2) 0)
               :$in in-fn :$nin nin-fn
               :$fn (fn [v f] (f v))}
          _and (fn [a b] (and a b))]
      (fn [row]
        (reduce _and
                (for [k (keys query-map)]
                  (if (map? (query-map k))
                    (reduce _and
                            (for [sk (keys (query-map k))]
                              (cond
                               (fn? sk)
                                 (sk (row k) ((query-map k) sk))
                               (nil? (ops sk))
                                 (throw (Exception. (str "Invalid key in query-map: " sk)))
                               :else
                                ((ops sk) (row k) ((query-map k) sk)))))
                    (= (row k) (query-map k)))))))))