(ns rss-slurper.system
  (:require [com.stuartsierra.component :as component]
            [rss-slurper.db :as db]
            [rss-slurper.news :as news]
            [rss-slurper.nlp :as nlp]
            [rss-slurper.rss :as rss]
            [rss-slurper.http-server :as web]))
(defn system
  [{:keys [db-port nlp-url rss-url-map web-port]}]
  (->
   (component/system-map
    :db (db/new-database db-port)
    :nlp (nlp/new-nlp-client nlp-url)
    :rss (rss/new-rss-controller rss-url-map)
    :web (web/new-webserver web-port))
   (component/system-using
    {:rss {:nlp :nlp
           :db  :db}
     :web {:db :db
           :rss :rss}})))
           

(defn print-feeds [{:keys [rss] :as sys}]
  (rss/print-feeds rss))

(defn save-feeds [{:keys [rss] :as sys}]
  (rss/save-all-feeds rss))


(defn count-items [{:keys [db] :as sys}]
  (db/get-total-news-item-count db))
