(ns jeroenvandijk.edntail.cli
  (:require [babashka.cli :as cli]
            [jeroenvandijk.edntail.api :as api]))


(def cli-opts
  {:coerce {:F :boolean
            :f :boolean
            :n :long}})


(defn -main [& cli-args]
  (let [{:keys [opts args]} (cli/parse-args cli-args cli-opts)]
    (when-let [unused-args (seq (if (:file opts) args (rest args)))]
      (println "WARN: unused args" (clojure.string/join " " unused-args)))

    (api/tail (merge {:file (first args)} opts))))


#?(:bb
   (apply -main *command-line-args*))
