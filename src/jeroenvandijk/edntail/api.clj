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


(defn tail-file->reader [file {:keys [follow Follow]}]
  (if (.exists (io/file file))
    (java.io.PushbackReader. (io/reader (:out (apply process {:err :inherit}
                                                     (concat ["tail"] (cond
                                                                        Follow ["-F"]
                                                                        follow ["-f"]) [file])))))
    (throw (ex-info "File does not exist" {:babashka/exit 1}))))


(defn template->output-fn [template]
  (let [render (requiring-resolve 'selmer.parser/render)]
    (fn [x]
      ;; REVIEW can this be optimized by preprocessing? https://github.com/yogthos/Selmer/blob/master/src/selmer/template_parser.clj
      (println (render template x)))))


(defn query->transform-fn [query]
  ((requiring-resolve 'jeroenvandijk.edntail.impl.query/query-transform) query))


(defn tail [{:keys [file query transform template]}]
  (let [rdr (cond
              (.ready *in*) *in*

              file (tail-file->reader file (select-keys args [:follow :Follow]))

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
