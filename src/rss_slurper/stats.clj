(ns rss-slurper.stats
  (:require [com.stuartsierra.component :as component :refer [Lifecycle start stop]]
            [clojure.tools.logging :as log]
            [rss-slurper.db :as db :refer-macros [with-news-items]]
            [monger.query :as q]
            [monger.collection :as mc]
            [monger.operators :refer :all]))
(defrecord StatsService [db]
  Lifecycle
  (start [this]
    (log/info "starting stats service")
    this)

  (stop [this]
    (log/info "stopping stats service")
    this))


(defn new-stats-service []
  (map->StatsService {}))

(defn find-by-source [this source]
  (db/with-news-items (:db this)
    (q/find {:source {$all source}})))

(defn sources-primary [this]
  (->>
   (db/aggregate-news-items
    (:db this)
    [{$group {:_id nil
              :source {$addToSet "$source"}}}])
   first
   :source
   (map first)
   (into #{})))
