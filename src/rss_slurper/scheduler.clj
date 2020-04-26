(ns rss-slurper.scheduler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async :refer [<! >! go go-loop chan]]
            [rss-slurper.db :as db]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as qj]
            [clojurewerkz.quartzite.triggers :as qt]))
(def hours-between-fetches 6)
(def hour-msecs (* 1000 60 60))

;; (qj/defjob SaveFeedsJob
  ;; [ctx]
  ;; (let [m (qj/)]))
(defrecord Scheduler [rss db]
  component/Lifecycle
  (component/start [this]
    (log/info "Starting scheduler")
    this)
  (component/stop [this]
    (log/info "Stopping scheduler")
    (this)))

(defn new-scheduler []
  (map->Scheduler {}))

(defn fetch-feeds? [{:keys [db]}]
  (<= hours-between-fetches (db/hours-since-last-item db)))

;; (defn start-scheduler [this]
  ;; (go-loop [r (<! (async/time))]))
