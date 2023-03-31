(ns jeroenvandijk.edntail.api
  (:require [clojure.java.io :as io]
            [puget.printer :as puget]
            [babashka.process :refer [process]]))


(defn edn-seq
  [rdr]
  (when-let [edn-val
             (try
               (clojure.edn/read {:eof ::EOF
                                  :default tagged-literal}
                                 rdr)
               (catch Exception ex
                 {::exception ex}))]
    (when-not (identical? ::EOF edn-val)
      (cons edn-val (lazy-seq (edn-seq rdr))))))


(def tail-opts
  [:f :F :n])


(defn tail-file->reader [file opts]
  (if (.exists (io/file file))
    (let [cmd (concat ["tail"]
                      (into []
                            (comp
                             (keep (fn [k]
                                     (when-let [v (get opts k)]
                                       (if (boolean? v)
                                         [(str "-" (name k))]

                                         [(str "-" (name k)) (str v)]))))
                             cat)
                            tail-opts)
                      [file])]
      (java.io.PushbackReader. (io/reader (:out (apply process {:err :inherit}
                                                       cmd)))))
    (throw (ex-info "File does not exist" {:babashka/exit 1}))))


(let [*f (delay (requiring-resolve 'jeroenvandijk.edntail.impl.template/template->output-fn))]
  (defn template->output-fn [template]
    (@*f template)))


(defn query->transform-fn [query]
  ((requiring-resolve 'jeroenvandijk.edntail.impl.query/query-transform) query))


(defn tail [{:keys [file query transform template] :as args}]
  (let [rdr (cond
              file (tail-file->reader file (select-keys args tail-opts))

              (.ready *in*) *in*

              :else
              (do (println "INFO: stdin not ready, maybe waiting for input")
                  *in*))

        output-fn
        (cond
          template (template->output-fn template)
          :default
          (fn [x]
            (puget/cprint x)
            (println)))

        transform-xf (cond
                      query (query->transform-fn query)

                      :default (comp))]
    ;; We shouldn't use doseq as it will appy batching
    ;; and doesn't eagerly consume the sequence
    (transduce transform-xf
               (fn [_ row] (output-fn row))
               identity
               (edn-seq rdr))))
