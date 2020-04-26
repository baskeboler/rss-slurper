(ns rss-slurper.rss
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [clojure.tools.logging :as log]
            [rss-slurper.nlp :as nlp]
            [rss-slurper.db :as db]
            [rss-slurper.message-bus :as mbus]
            [rss-slurper.news :as news]
            [com.rpl.specter :as s]))
(defrecord RSSController [nlp db url-map bus]
  component/Lifecycle
  (component/start [this]
    (log/info "starting RSS controller")
    this)
  (component/stop [this]
    (log/info "stopping RSS controller")
    this))

(defn new-rss-controller [urls]
  (map->RSSController {:url-map urls}))

(defn analyze-text [ctrl t]
  (nlp/analyze-text (:nlp ctrl) t))

(def doc-walker
  (s/recursive-path
   []
   p
   (s/if-path #(= :item (:tag %))
              s/STAY
              [:content s/ALL p])))

(defn get-feed [{:keys [nlp db] :as ctrl} path]
  (let [url (get-in (:url-map ctrl) path)]
    (some->> url
             (http/get)
             :body
             xml/parse-str
             (s/select  [doc-walker])
             (s/transform [s/ALL] (comp (partial news/analyze nlp) (partial news/item->news path))))))


(defn save-all-feeds [{:keys [bus db url-map] :as this}]
  (mbus/notification! bus {:message "starting to save all feeds" :type :save-all-feeds})
  (doseq [k1 (keys url-map)]
    (doseq [k2   (keys (get url-map k1))
            :let [f (get-feed this [k1 k2])]]
      (db/save-feed db f)
      (mbus/notification! bus {:message "feed saved" :source [k1 k2] :type :feed-saved})
                              
      (log/info "feeds saved")))
  (mbus/notification! bus {:message "finished saving all feeds" :type :save-all-feeds}))



(defn print-feeds [{:keys [bus url-map] :as this}] 
  (doseq [k1 (keys url-map)]
    (doseq [k2 (keys (get url-map k1))]
      (pprint (get-feed this [k1 k2])))))
