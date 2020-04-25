(ns rss-slurper-ui.core
  (:require [reagent.core :as reagent]
            [reagent.dom :refer [render]]
            [re-frame.core :as rf]
            [clojure.string :as cstr] 
            [rss-slurper-ui.events :as events]
            [rss-slurper-ui.subs :as subs]
            [rss-slurper-ui.routing :as routing :refer [app-routes!]]
            [rss-slurper-ui.navbar :refer [navbar]]))
(defn- format-date [date-str]
  (.. (js/Date. date-str)
      (toUTCString)))

(defn- source-badges [[a b]]
 [:div
  [:span.badge.badge-primary.m-1.p-1 a]
  [:span.badge.badge-secondary.m-1.p-1 b]])

(defn- avg [& ns]
  (/ (* 1.0 (apply + ns))
     (count ns)))

(defn sentiment [item]
  (->> item
      :analysis
      :description
      :sentences
      (map :sentimentValue)
      (map #(js/parseInt  %))
      (apply avg)
      str))

(defn item-table [items]
  [:table.table.table-sm.table-hover
   [:thead>tr
    [:th "date"]
    [:th "title"]
    [:th "source"]
    [:th "sentiment"]]
   [:tbody
    (doall
     (for [i (->> @items vals (sort-by :date-published) reverse)]
       ^{:key (str "item-" (:_id i))}
       [:tr
        {:on-click #(rf/dispatch [::events/set-selected-item i])}
        [:td (-> i :date-published format-date)]
        [:td (:title i)]
        [:td  (source-badges (:source i))]
        [:td (sentiment i)]]))]])

(defn home-view []
  (let [items         (rf/subscribe [::subs/news-items])
        selected-item (rf/subscribe [::subs/selected-item])]
    [:div.container
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
         [:a.btn.btn-outline-secondary.text-capitalize.btn-block
          {:href (:link @selected-item)
           :target :_blank}         
          "go to article"]
         [:button.btn.btn-outline-primary.text-capitalize.btn-block
          {:on-click #(rf/dispatch [::events/set-selected-item nil])}
          "dismiss"]])]]))

(defn stats-view []
  [:div.container
   [:h1.h1 "Stats"]
   [:p "or so they say"]
   [:div.row>div.col
    [:table.table.table-hover.table-sm
     [:thead
      [:tr
       [:th "Source"]
       [:th "#"]]]
     [:tbody
      (doall
       (for [[source item-count] (some->> @(rf/subscribe [::subs/stats]) :items-by-source (sort-by first))]
         ^{:key (str "item_" (hash source))}
         [:tr
          [:td source]
          [:td item-count]]))]
     [:tfoot
      [:tr
       [:th "Total"]
       [:td (some-> @(rf/subscribe [::subs/stats]) :total-count)]]]]]])


(defn app-component []
  (let [current-view         (rf/subscribe [::subs/current-view])]
    (fn []
      [:div.app-component
       [navbar]
       (condp = @current-view
         :home [home-view]
         :stats [stats-view]
         [home-view])])))

(defn mount-components! []
  (routing/app-routes!)
  (rf/dispatch-sync [::events/init])
  (rf/dispatch [::events/get-news-items])
  (rf/dispatch [::events/get-stats]))
(defn ^:export init! []
  (println "init function!")
  (mount-components!)
  (render [app-component]
          (.getElementById js/document "app")))
