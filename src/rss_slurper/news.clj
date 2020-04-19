(ns rss-slurper.news
  (:require [com.rpl.specter :as s :include-macros true]
            [clojure.string :as str])
  (:import [java.util Date]))

(defprotocol Analyzer
  (analyze [this data]))



(defrecord NewsItem [title description link date-published source analysis identity])
  ;; Analyzable
  ;; (analyze [this client]
    ;; (-> this
        ;; (assoc :analysis {:title       (nlp/analyze-text client title)
                          ;; :description (nlp/analyze-text client description)]))

(defn news-item-identity [{:keys [title description source link date-published] :as item}]
  (hash {:title          title
         :description    description
         :source         source
         :link           link
         :date-published date-published}))

(defn- safe-trim [s]
  (if-not (nil? s)
    (str/trim s)
    ""))

(defn item->news [source i]
  (let [{:keys [content]} i
        title             (s/select-one [s/ALL #(= :title (:tag %)) :content s/FIRST] content)
        description       (s/select-one [s/ALL #(= :description (:tag %)) :content s/FIRST] content)
        link              (s/select-one [s/ALL #(= :link (:tag %)) :content s/FIRST] content)
        date-published    (safe-trim
                           (s/select-one [s/ALL #(= :pubDate (:tag %)) :content s/FIRST] content))
        date-published    (if-not (str/blank? date-published)
                            (Date. (Date/parse date-published))
                            nil)]

    (let [item (map->NewsItem
                {:title          (safe-trim title)
                 :description    (safe-trim description)
                 :link           (safe-trim link)
                 :date-published date-published
                 :source         source})]
      (-> item
          (assoc :identity (news-item-identity item))))))
