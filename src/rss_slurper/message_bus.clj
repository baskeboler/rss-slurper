(ns rss-slurper.message-bus
  (:require [com.stuartsierra.component :as component :refer [Lifecycle start stop]]
            [manifold.bus :as bus]
            [clojure.tools.logging :as log]
            [clj-time.core :as ctime]
            [clj-time.format :as time-format]))
(defrecord MessageBus []
  Lifecycle
  (start [this]
    (log/info "starting message bus")
    (-> this
        (assoc :bus (bus/event-bus))))
  (stop [this]
    (log/info "stopping message bus")
    (-> this
        (dissoc :bus))))

(defn new-message-bus []
  (map->MessageBus {}))

(defn- with-timestamp [m]
  (if (map? m)
    (-> m (assoc :_timestamp (.toDate (ctime/now))))
    m))
(defn publish-message! [this topic m]
  (bus/publish! (:bus this) topic
                (if (string? m)
                  m
                  ((comp pr-str with-timestamp) m))))

(defn subscribe [this topic]
  (bus/subscribe (:bus this) topic))

(defn notification! [this notification]
  (publish-message! this "default-notifications" notification))
