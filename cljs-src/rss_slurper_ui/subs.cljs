(ns rss-slurper-ui.subs
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))


(defn- ceil [n] (.ceil js/Math n))

(defn- reg-simple-sub [kw db-key]
  (rf/reg-sub
   kw
   (fn [db _]
     (get db db-key))))

;; (rf/reg-sub
 ;; ::news-items
 ;; (fn
  ;; [db _]
  ;; (:news-items db))

(reg-simple-sub ::news-items :news-items)
(reg-simple-sub ::items-per-page :items-per-page)

;; (rf/reg-sub
 ;; ::items-per-page
 ;; (fn [db _] (:items-per-page db)))


(rf/reg-sub
 ::news-items-count
 :<- [::news-items]
 (fn
  [items _]
  (count (vals items))))

(rf/reg-sub
 ::page-count
 :<- [::news-items-count]
 :<- [::items-per-page]
 (fn [[a b] _]
  (ceil (/ a b))))

;; (rf/reg-sub
 ;; ::selected-item
 ;; (fn
  ;; [db _]
  ;; (:selected-item db)))

;; (rf/reg-sub
 ;; ::current-view
 ;; (fn [db _] (:current-view db)))

;; (rf/reg-sub
 ;; ::stats
 ;; (fn [db _] (:stats db)))

(reg-simple-sub ::selected-item :selected-item)
(reg-simple-sub ::current-view :current-view)
(reg-simple-sub ::stats :stats)
(reg-simple-sub ::sources :sources)
