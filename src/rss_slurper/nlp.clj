(ns rss-slurper.nlp
  (:require [com.stuartsierra.component :as component]
            [clj-http.client :as http]
            [clojure.string :as str]
            [rss-slurper.news :as news]
            [clojure.tools.logging :as log]))

(defrecord NLPClient [url]
  component/Lifecycle
  (component/start [this]
    (log/info "starting nlp client")
    this)
  (component/stop [this]
    (log/info "stopping nlp client")
    this))


(def nlp-host "http://localhost:9991")


(defn new-nlp-client [url]
  (map->NLPClient {:url url}))


(defn analyze-text
  ([client t]
   (analyze-text client t [:lemma :ner :tokenize,:ssplit,:pos,:sentiment]))
  ([{:keys [url] :as client} t props]
   (:body
    (http/post (str url (format "?properties={\"annotators\":\"%s\",\"outputFormat\":\"json\"}" (str/join "," (map name props))))
               {:body t
                :as :json}))))

(defn analyze-sentiment [client t]
  (analyze-text client t [:sentiment]))


(extend-protocol news/Analyzer
  NLPClient
  (news/analyze [this item]
    (-> item
        (assoc :analysis {:title       (analyze-text this (:title item))
                          :description (analyze-text this (:description item))}))))
