(ns rss-slurper-ui.navbar
  (:require [re-frame.core :as rf]
            [rss-slurper-ui.subs :as subs]
            [rss-slurper-ui.routing :as routing]))


(defn nav-link [name label]
  [:a.nav-item.nav-link
   {:href (routing/url-for name)
    :class (when (= name @(rf/subscribe [::subs/current-view])) "active")}
   label])

(defn ^:export navbar []
  [:nav.navbar.navbar-expand-lg.navbar-light.bg-light
   [:a.navbar-brand
    {:href "#"}
    "RSS"]
   [:button.navbar-toggler
    {:type          :button
     :aria-controls "theNavs"
     :aria-expanded false
     :aria-label    "Toggle Navigation"
     :data-target   "#theNavs"
     :data-toggle   "collapse"}
    [:span.navbar-toggler-icon]]
   [:div#theNavs.collapse.navbar-collapse
    [:div.navbar-nav
     [nav-link :home "Home"]
     [nav-link :stats "Stats"]]]])
     ;; [:a.nav-item.nav-link
      ;; {:href (routing/url-for :home)}
      ;; "Home"]
     ;; [:a.nav-item.nav-link
      ;; {:href (routing/url-for :stats)}
      ;; "Stats"]]]])
