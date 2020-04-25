(ns rss-slurper-ui.routing
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as rf]
            [rss-slurper-ui.events :as events]))

(defmulti handle-route "route handling multimethod" :action)
(defmethod handle-route :default [arg] (:action arg))

(defprotocol ViewDispatcher
  "View dispatcher protocol"
  (dispatch-view [this]))

(extend-protocol ViewDispatcher
  cljs.core.Keyword
  (dispatch-view [this]
    (rf/dispatch [::events/set-current-view this])))

(def routes
  ["/" {""      :home
        "stats" :stats}])


(defn- parse-url [url]
  (bidi/match-route routes url))

(defn create-route-data [handler params]
  {:action handler
   :params params})

(defn dispatch-route
  [{:keys [route-params handler] :as matched-route}]
  (println matched-route)
  (let [route-data (create-route-data handler route-params)
        view (handle-route route-data)]
    (dispatch-view view)))

(defonce ^:export router (pushy/pushy dispatch-route parse-url))

(defn app-routes! []
  (pushy/start! router))

(def url-for (partial bidi/path-for routes))
