(ns rss-slurper-ui.subs
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-sub
 ::news-items
 (fn-traced
  [db _]
  (:news-items db)))


(rf/reg-sub
 ::selected-item
 (fn-traced
  [db _]
  (:selected-item db)))

(rf/reg-sub
 ::current-view
 (fn-traced [db _] (:current-view db)))

(rf/reg-sub
 ::stats
 (fn-traced [db _] (:stats db)))
