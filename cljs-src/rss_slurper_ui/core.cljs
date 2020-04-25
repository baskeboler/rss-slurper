(ns rss-slurper-ui.core
  (:require [reagent.core :as reagent]
            [reagent.dom :refer [render]]
            [re-frame.core :as rf]
            [clojure.string :as cstr] 
            [rss-slurper-ui.events :as events]
            [rss-slurper-ui.subs :as subs]))

(defn item-table [items]
  [:table.table.table-hover
   [:thead>tr
    [:th "date"]
    [:th "title"]
    [:th "source"]]
   [:tbody
    (doall
     (for [[item-id i] @items]
       ^{:key (str "item-" item-id)}
       [:tr
        {:on-click #(rf/dispatch [::events/set-selected-item i])}
        [:td (:date-published i)]
        [:td (:title i)]
        [:td (cstr/join ", " (:source i))]]))]])

(defn app-component []
  (let [items         (rf/subscribe [::subs/news-items])
        selected-item (rf/subscribe [::subs/selected-item])]
    (fn []
      [:div.app-component.container
       [:h1.h1 "rss slurper ui"]
       [:p "hey there!"]
       [:div.row
        [:div.col
         [item-table items]]
        (when-not (nil? @selected-item)
          [:div.col
           [:h3 (:title @selected-item)]
           [:h4 (cstr/join " - " (:source @selected-item))]
           [:p (:description @selected-item)]
           [:button.btn.btn-primary
            {:on-click #(rf/dispatch [::events/set-selected-item nil])}
            "dismiss"]])]])))

(defn mount-components! []
  (rf/dispatch-sync [::events/init])
  (rf/dispatch [::events/get-news-items]))

(defn ^:export init! []
  (println "init function!")
  (mount-components!)
  (render [app-component]
          (.getElementById js/document "app")))
