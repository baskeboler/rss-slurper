(ns dev
  (:require [rss-slurper.system :refer [system]]
            [rss-slurper.db :as db]
            [rss-slurper.rss :as rss]
            [monger.conversion :refer :all]
            [monger.operators :refer :all]
            [clj-time.core :as ctime]
            [core :as core]
            [clojure.tools.cli :as cli]
            [com.stuartsierra.component :as component]))

(def ^:dynamic *system* nil)

(def opts (cli/parse-opts ["app" "listen"] core/cli-options))


(defn start-system []
  (if-not (nil? *system*)
    *system*
    (let [{:keys [nlp-url port web-port]} (:options opts)
          sys
          (system {:db-port     port
                   :nlp-url     nlp-url
                   :rss-url-map core/default-urls
                   :web-port    web-port})]
      (alter-var-root
       #'*system*
       (constantly
        (component/start-system sys))))))

(defn stop-system []
  (if-not  (nil? *system*)
    (do
      (component/stop-system *system*)
      (alter-var-root #'*system* (constantly nil)))
    *system*))

(defn restart []
  (stop-system)
  (start-system))
