(ns rss-slurper.rss
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clj-http.client :as http]
            [clojure.data.xml :as xml]
            [clojure.tools.logging :as log]
            [rss-slurper.nlp :as nlp]
            [rss-slurper.db :as db]
            [rss-slurper.news :as news]
            [com.rpl.specter :as s]))
(defrecord RSSController [nlp db url-map]
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


(defn save-all-feeds [c]
  (doseq [k1 (keys (:url-map c))]
    (doseq [k2   (keys (get (:url-map c) k1))
            :let [f (get-feed c [k1 k2])]]
      (db/save-feed (:db c) f)
      (log/info "feeds saved"))))



(defn print-feeds [this] 
  (doseq [k1 (keys (:url-map this))]
    (doseq [k2 (keys (get (:url-map this) k1))]
      (pprint (get-feed this [k1 k2])))))
