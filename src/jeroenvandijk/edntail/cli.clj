(ns jeroenvandijk.edntail.cli
  (:require [babashka.cli :as cli]
            [jeroenvandijk.edntail.api :as api]))


(def cli-opts
  {:alias {:F :Follow
           :f :follow}})


(defn -main [& args]
  (api/tail (:opts (cli/parse-args args cli-opts))))


#?(:bb
   (apply -main *command-line-args*))
