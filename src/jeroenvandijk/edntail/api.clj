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


(defn template->output-fn [template]
  (let [render (requiring-resolve 'selmer.parser/render)]
    (fn [x]
      ;; REVIEW can this be optimized by preprocessing? https://github.com/yogthos/Selmer/blob/master/src/selmer/template_parser.clj
      (println (render template x)))))


(defn query->transform-fn [query]
  ((requiring-resolve 'jeroenvandijk.edntail.impl.query/query-transform) query))


(defn tail [{:keys [file query transform template] :as args}]
  (let [rdr (cond
              (.ready *in*) *in*

              file (tail-file->reader file (select-keys args tail-opts))

              :else
              (throw (ex-info "No input given" {:babashka/exit 1})))

        output-fn
        (cond
          template (template->output-fn template)
          :default
          (fn [x]
            (puget/cprint x)
            (println)))

        tranform-xf (cond
                      query (query->transform-fn query)
                      :default identity)]
    (doseq [row (sequence tranform-xf (edn-seq rdr))]
      (output-fn row))))
