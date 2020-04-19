(ns rss-slurper.http-server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params  :refer [wrap-keyword-params]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            ;; [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            ;; [com.rpl.specter :as s]
            [rss-slurper.db :as db]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.middleware :refer []]
            [clojure.tools.logging :as log]))

(defn json-resp [body]
  {:status 200
   :headers {"content-type" "application/json"}
   :body body})

(defn http-handler [request]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "Hello world"})

(defn get-items [{:keys [server]}]
  (log/info "serveR? " server)
  {:status 200
   :headers {"content-type" "application/json"}
   :body (map
          #(update %  :_id str)
          (db/get-last-news-summaries (:db server)))})

(defn get-item-by-id [obj-id]
  (fn [{:keys [server]}]
    (let [ret (db/find-by-id (:db server) obj-id)]
      {:status       200
       :headers {"content-type" "application/json"}
       :body
       (-> ret
           (update :_id str))})))
(defn sources [{:keys [server]}]
  (log/info "fetching count by sources")
  (json-resp (db/get-count-by-sources  (:db server))))

(defroutes app
  (GET "/" [] "<h1>Hello World</h1>")
  (GET "/items" [] get-items)
  (GET "/items/:obj-id" [obj-id] (get-item-by-id obj-id))
  (GET "/sources" [] sources)
  (route/not-found "<h1>Page not found</h1>"))

(defn wrap-server [handler server]
  (fn [req]
    (handler (assoc req :server server))))

(defrecord WebServer [db rss port join?]
  component/Lifecycle
  (component/start [this]
    (log/info "starting web server")
    (-> this
        (assoc :server (jetty/run-jetty
                        (-> app
                            (wrap-params)
                            (wrap-keyword-params)
                            (wrap-content-type)
                            (wrap-json-response)
                            (wrap-json-body)
                            (wrap-server this))
                        {:port port :join? join?}))))
  (component/stop [this]
    (log/info "stopping web server")
    (.stop (:server this))
    (-> this
        (dissoc :server))))

(defn new-webserver
  ([port join?]
   (map->WebServer {:port port :join? join?}))
  ([port]
   (new-webserver port false)))
