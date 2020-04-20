(ns rss-slurper.db
  (:require [com.stuartsierra.component :as component]
            [monger.core :as mg]
            [monger.operators :refer :all]
            [monger.conversion :as mconv]
            [monger.query :as q]
            [monger.collection :as mc]
            [rss-slurper.news :refer [news-item-identity]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clj-time.core :as ctime]))
            

(defrecord Database [port host]
  component/Lifecycle
  (component/start [this]
    (log/info "starting database")
    (-> this
        (assoc :conn (mg/connect {:port port}))))
  (component/stop [this]
    (log/info "stopping database")
    (mg/disconnect (:conn this))
    (-> this
        (dissoc :conn))))

(def news-db "news")
(def news-items-collection "news-items")

(defn new-database [port]
  (map->Database {:port port}))
(defn get-db [db-comp]
  (mg/get-db (:conn db-comp) news-db))

(defn save-feed [c feed]
  (when-let [db (get-db c)]
    (doall
     (doseq [item feed]
       (mc/update
        db "news-items"
        {:identity (news-item-identity item)}
        {$set item}
        {:upsert true
         :multi  false})))))
       ;; (queue/enquene-message {:type :new-item
                               ;; :item item})))))


(defn- avg [ns]
  (/ (* 1.0 (reduce + 0 ns))
     (count ns)))
(defn sentiment-score [analysis]
  (->> analysis
       :sentences
       (map :sentimentValue)
       avg))
(defn- parse-int [n]
  (Integer/parseInt n))
(defn- update-sentiment-score [analysis-key item]
  (-> item
      (assoc :sentiment-score
             (->> item
                  :analysis
                  (#(get % analysis-key))
                  :sentences
                  (map (comp parse-int :sentimentValue))
                  avg))
      (dissoc :analysis)))
(def summary-proyection
  {:title 1
   :date-published 1
   :source 1})

(def summary-fields
  [:title :source :description
   :date-published
   :analysis.description.sentences.sentimentValue
   :analysis.description.sentences.sentiment])

(defn get-all-news-item-summaries [this]
  (q/with-collection  (get-db this) news-items-collection
    (q/find {})
    (q/fields summary-fields)
    (q/sort  (array-map :date-published 1))))

(defn get-last-news-summaries
  ([this n]
   (q/with-collection (get-db this) news-items-collection
     (q/find {})
     (q/fields summary-fields)
     (q/sort (array-map :date-published -1))
     (q/limit n)))
  ([this]
   (get-last-news-summaries this 100)))

(defn get-filtered-summaries
  ([this filter-map limit]
   (q/with-collection (get-db this) news-items-collection
     (q/find filter-map)
     (q/fields summary-fields)
     (q/sort (array-map :date-published -1))
     (q/limit limit))))

(defn get-summaries-with-score
  ([this n]
   (->> (get-last-news-summaries this n)
        (map (partial update-sentiment-score :description))))
  ([this]
   (get-summaries-with-score this 100)))

(defn get-total-news-item-count [this]
  (mc/count  (get-db this) news-items-collection))

(defn delete-all-items [this]
  (mc/remove  (get-db this) news-items-collection))

(defn find-by-id [component ^String obj-id]
  (let [object-id (mconv/to-object-id obj-id)]
    (mc/find-map-by-id (get-db component) news-items-collection object-id)))

(defn get-count-by-sources [this]
  (let [docs
        (q/with-collection (get-db this) news-items-collection
          (q/find {})
          (q/fields [:source]))
        grouped (group-by :source docs)]
    (->> (for [[k v] grouped]
           [(str/join " - " k) (count v)])
         (into (sorted-map)))))


(defn get-summaries-newer-than-n-hours
  ([this hours limit] 
   (let [since-when (-> (ctime/now)
                        (ctime/minus- (ctime/hours hours)))]
     (get-filtered-summaries
      this
      {:date-published {$gte (.toDate since-when)}}
      limit)))
  ([this hours]
   (get-summaries-newer-than-n-hours this hours 1000)))
