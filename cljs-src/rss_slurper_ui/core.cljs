(ns rss-slurper-ui.core
  (:require [reagent.core :as reagent]
            [ajax.core :refer [GET POST]]
            [clojure.string :as cstr])) 


(defn get-items [items-atom]
  (GET "/last-hours/7" {:response-format :json
                        :handler #(reset! items-atom %)
                        :keywords? true}))

(defn item-table [items]
  [:table.table.table-striped
   [:thead>tr
    [:th "date"]
    [:th "title"]
    [:th "source"]]
   [:tbody
    (doall
     (for [i @items]
       ^{:key (str "item-" (:_id i))}
       [:tr
        [:td (:date-published i)]
        [:td (:title i)]
        [:td (cstr/join ", " (:source i))]]))]])

(defn app-component []
  (let [items (reagent/atom [])]
    (get-items items)
    (fn []
      [:div.app-component.container
       [:h1 "rss slurper ui"]
       [:p "hey there!"]
       [item-table items]])))


(defn ^:export init! []
  (println "init function!")
  (reagent/render [app-component]
                  (.getElementById js/document "app")))
