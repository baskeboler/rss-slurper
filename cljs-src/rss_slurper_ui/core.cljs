(ns rss-slurper-ui.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [ajax.core :refer [GET POST]]
            [clojure.string :as cstr])) 


(defn get-items [items-atom]
  (GET "/last-hours/6" {:response-format :json
                        :handler #(reset! items-atom %)
                        :keywords? true}))

(defn get-item [item-id item-atom]
  (GET (str "/items/" item-id) {:response-format :json
                                :handler         #(reset! item-atom %)
                                :keywords?       true}))

(defn item-table [items selected]
  [:table.table.table-hover
   [:thead>tr
    [:th "date"]
    [:th "title"]
    [:th "source"]]
   [:tbody
    (doall
     (for [i @items]
       ^{:key (str "item-" (:_id i))}
       [:tr
        {:on-click #(get-item (:_id i) selected)}
        [:td (:date-published i)]
        [:td (:title i)]
        [:td (cstr/join ", " (:source i))]]))]])

(defn app-component []
  (let [items         (reagent/atom [])
        selected-item (reagent/atom nil)]
    (get-items items)
    (fn []
      [:div.app-component.container
       [:h1.h1 "rss slurper ui"]
       [:p "hey there!"]
       [:div.row
        [:div.col
         [item-table items selected-item]]
        (when-not (nil? @selected-item)
          [:div.col
           [:h3 (:title @selected-item)]
           [:h4 (cstr/join " - " (:source @selected-item))]
           [:p (:description @selected-item)]
           [:button.btn.btn-primary
            {:on-click #(reset! selected-item nil)}
            "dismiss"]])]])))

;; (defn mount-components! []
  ;; (rf/))
(defn ^:export init! []
  (println "init function!")
  (reagent/render [app-component]
                  (.getElementById js/document "app")))
