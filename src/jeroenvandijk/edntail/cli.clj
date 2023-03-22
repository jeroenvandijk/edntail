(ns jeroenvandijk.edntail.cli
  (:require [babashka.cli :as cli]
            [jeroenvandijk.edntail.api :as api]))


(def cli-opts
  {:coerce {:Follow :boolean
            :follow :boolean}
   :alias {:F :Follow
           :f :follow}})


(defn -main [& cli-args]
  (let [{:keys [opts args]} (cli/parse-args cli-args cli-opts)]
    (api/tail (assoc opts :file (first args)))))


#?(:bb
   (apply -main *command-line-args*))
