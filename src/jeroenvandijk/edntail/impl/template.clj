(ns jeroenvandijk.edntail.impl.template
  (:require [clojure.term.colors :as c]
            [selmer.parser :as selmer :refer [render]]))


(defn apply-styling [text choice options]
  (if choice
    (if-let [f (get options (name choice))]
      (f text)
      (throw (ex-info (str "No option " choice) {})))
    text))


(defn with-style [text opts]
  (if-not opts
    text
    (let [opts (clojure.walk/keywordize-keys (apply hash-map opts))]
      ;; TODO other styling options
      #_ ["concealed"
           "reverse-color"
           "blink"
           "underline"
           "dark"
          "bold"]

      (-> text
          (apply-styling (:text opts)
                         {"white" c/white
                          "cyan" c/cyan
                          "magenta" c/magenta
                          "blue" c/blue
                          "yellow" c/yellow
                          "green" c/green
                          "red" c/red
                          "grey" c/grey})

          (apply-styling (:background opts)
                         {"white"   c/on-white
                          "cyan" c/on-cyan
                          "magenta" c/on-magenta
                          "blue" c/on-blue
                          "yellow" c/on-yellow
                          "green" c/on-green
                          "red" c/on-red
                          "grey" c/on-grey})))))


;; See https://github.com/yogthos/Selmer#tags
(selmer/add-tag! :with-style
          (fn [args _ {{:keys [content]} :with-style}]
            (with-style content args))
          :end-with-style)


;; See https://github.com/yogthos/Selmer#filters
(selmer/add-filter! :style   (fn [text args]
                               (with-style text
                                 (-> args
                                     (clojure.string/trim)

                                     (clojure.string/split #" ")))))


(defn template->output-fn [template]
  (fn [row]
      ;; REVIEW can this be optimized by preprocessing? https://github.com/yogthos/Selmer/blob/master/src/selmer/template_parser.clj
    (println (render template row))))
