(ns rss-slurper.http-server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params  :refer [wrap-keyword-params]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            ;; [com.rpl.specter :as s]
            [rss-slurper.db :as db]
            [rss-slurper.stats :as stats]
            [compojure.core :refer :all]
            [ring.util.response :refer [resource-response content-type]]
            [compojure.route :as route]
            [compojure.middleware :refer []]
            [compojure.coercions :refer :all]
            [clojure.tools.logging :as log]
            [aleph.http :as aleph-http]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [rss-slurper.message-bus :as mbus]))

(def non-websocket-request
  {:status 400
   :headers {"content-type" "application/text"}
   :body "Expected websocket request"})


(defn json-resp [body]
  {:status 200
   :headers {"content-type" "application/json"}
   :body body})

(defn http-handler [request]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "Hello world"})

(defn get-items [{:keys [server]}]
  ;; (log/info "server? " server)
  (json-resp
   (map
    #(update %  :_id str)
    (db/get-last-news-summaries (:db server)))))

(defn get-item-by-id [obj-id]
  (fn [{:keys [server]}]
    (let [ret (db/find-by-id (:db server) obj-id)]
      (json-resp
       (-> ret
           (update :_id str))))))

(defn get-latest-items [hours]
  (fn [{:keys [server]}]
    (json-resp
     (->>
      (db/get-summaries-newer-than-n-hours
       (:db server)
       hours)
      (map #(update % :_id str))))))

(defn sources-handler [{:keys [server]}]
  (json-resp
   (stats/sources-primary (:stats server))))

(defn stats [{:keys [server]}]
  (log/info "fetching count by sources")
  (let [item-count (db/get-total-news-item-count (:db server))
        count-by-sources (db/get-count-by-sources (:db server))
        response-object {:total-count item-count
                         :items-by-source count-by-sources}]
     (json-resp response-object)))

(defn echo-handler [req]
  (-> (aleph-http/websocket-connection req)
      (d/chain
       (fn [conn]
         (println "i got a connection!" conn)
         conn)
       (fn [socket]
         (s/connect socket socket))
       (fn [a]
         (println "this is after the handler" a)
         a))
      (d/catch
          (fn [_]
            (println "error connecting")
            non-websocket-request))))

(defn notifications-handler [{:keys [server]:as req}]
  (let [notifications-bus (:bus server)]
    (d/let-flow [conn (d/catch (aleph-http/websocket-connection req)
                          (fn [_] nil))]
                (if-not conn
                  non-websocket-request
                  (do
                    (mbus/publish-message! notifications-bus "default-notifications" (pr-str {:message "new ws connection" :type :debug}))
                    (s/connect (mbus/subscribe notifications-bus "default-notifications")
                               conn
                               {:timeout 1e4})
                    nil)))))

(defroutes app
  (GET "/" [] (->
               (resource-response "public/index.html")
               (content-type "text/html")))
  (GET "/items" [] get-items)
  (GET "/last-hours/:hours" [hours :<< as-int] (get-latest-items hours))
  (GET "/items/:obj-id" [obj-id] (get-item-by-id obj-id))
  (GET "/stats" [] stats)
  (GET "/echo" [] echo-handler)
  (GET "/sources" [] sources-handler)
  (GET "/notifications" [] notifications-handler)
  (route/not-found "<h1>Page not found</h1>"))

(defn wrap-server [handler server]
   (fn [req]
    (handler (assoc req :server server))))

(defrecord WebServer [db rss stats port join? bus]
  component/Lifecycle
  (component/start [this]
    (log/info "starting web server")
    (-> this
        (assoc :server
               ;; (jetty/run-jetty
               (aleph-http/start-server
                 (-> app
                    (wrap-params)
                    (wrap-keyword-params)
                    (wrap-content-type)
                    (wrap-json-response)
                    (wrap-json-body)
                    (wrap-server this)
                    (wrap-resource "/public"))
                {:port port :join? join?}))))
  (component/stop [this]
    (log/info "stopping web server")
    (.close (:server this))
    (-> this
        (dissoc :server))))

(defn new-webserver
  ([port join?]
   (map->WebServer {:port port :join? join?}))
  ([port]
   (new-webserver port false)))
