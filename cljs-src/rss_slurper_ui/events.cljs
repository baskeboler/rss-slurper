(ns rss-slurper-ui.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def initial-state
  {:news-items     {}
   :selected-item  nil
   :current-view   :home
   :items-per-page 10})

(rf/reg-event-db
 ::init
 (fn-traced [db _]
            initial-state))

(rf/reg-event-db
 ::set-selected-item
 (fn-traced [db [_ item]]
            (-> db
                (assoc :selected-item item))))

(rf/reg-event-db
 ::set-news-items
 (fn-traced
  [db [_ items]]
  (-> db
      (assoc :news-items items))))

(rf/reg-event-db
 ::set-current-view
 (fn-traced
  [db [_ view]]
  (-> db
      (assoc :current-view view))))

(declare resolve-error-evt-vec)

(rf/reg-event-fx
 ::get
 (fn-traced
  [{:keys [db] :as cofx} [_ arg]]
  (let [{:keys [url on-success on-error response-format]
         :or   {response-format (ajax/json-response-format {:keywords? true})
                on-error        ::generic-error-handler}} arg]
    {:db         db
     :http-xhrio {:method          :get
                  :uri             url
                  :timeout         8000
                  :response-format response-format
                  :on-success      [on-success]
                  :on-failure      (resolve-error-evt-vec on-error)}})))

(defn- resolve-error-evt-vec [on-error]
  (cond
    (keyword? on-error) [on-error]
    (vector? on-error) on-error
    :else (do
            (println "[WARNING] I don't know what this error handler is: " on-error)
            on-error)))

(rf/reg-event-fx
 ::generic-error-handler
 (fn-traced
  [{:keys [db]} [_ error]]
  (println "generic error handling: " error)
  {:db db}))

(rf/reg-event-fx
 ::get-news-items
 (fn-traced
  [{:keys [db] :as cofx} _]
  {:db db
   :dispatch [::get {:url "/last-hours/48"
                     :on-success ::get-news-items-success}]}))

(rf/reg-event-fx
 ::get-news-items-success
 (fn-traced
  [{:keys [db] :as cofx} [_ items]]
  (println "success")
  (let [items-map (into {}
                        (doall
                         (for [i items]
                           [(:_id i) i])))]
    {:db       db
     :dispatch [::set-news-items items-map]})))

(rf/reg-event-fx
 ::get-stats
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::get {:url "/stats"
                     :on-success ::get-stats-success}]}))

(rf/reg-event-db
 ::get-stats-success
 (fn-traced
  [db [_ stats]]
  (-> db
      (assoc :stats stats))))
