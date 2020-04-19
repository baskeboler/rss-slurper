(ns rss-slurper.message-queue
  (:require [taoensso.carmine.message-queue :as mq]
            [taoensso.carmine :as car :refer [wcar]]))

(def redis-conn {:pool {}
                 :spec {:uri "redis://localhost"}})
(defmacro wcar* [& body]
  `(wcar redis-conn ~@body))

(def news-queue "news-queue")

(defn ^:export enquene-message
  [message]
  (wcar*
   (mq/enqueue news-queue message)))


(def ^:dynamic *dummy-worker*)

(defn init-worker []
  (alter-var-root
   #'*dummy-worker*
   (constantly
    (mq/worker
     redis-conn
     news-queue
     {:handler (fn [{:keys [message attempt]}]
                 (println "Received " (:type message) " - " (-> message :item :title))
                 {:status :success})}))))               

;; (defonce dummy-worker
  ;; (mq/worker redis-conn news-queue
             ;; {:handler (fn [{:keys [message attempt]}]
                         ;; (println "Received " (:type message) " - " (-> message :item :title))
                         ;; {:status :success}})}))))

;; (alter-var-root)
