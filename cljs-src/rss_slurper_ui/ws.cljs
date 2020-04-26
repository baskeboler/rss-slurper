(ns rss-slurper-ui.ws
  (:require [chord.client :as ch]
            [cljs.core.async :as async :refer [>! <! close!]]))


(defn try-echo []
  (async/go
    (let [{:keys [ws-channel error]} (async/<! (ch/ws-ch  "ws://localhost:3000/echo"))]
      (if-not error
        (do
          (async/>! ws-channel "some test message!")
          (let [msg (async/<! ws-channel)] 
            (println "Got a message from the server! " msg))
          (async/close! ws-channel))
        (js/console.log "Error: " (pr-str error))))))

(defn subscribe-to-notifications []
  (async/go
    (let [{:keys [ws-channel error]} (<! (ch/ws-ch "ws://localhost:3000/notifications"))]
      (if-not error
        (do
          (println "connected to notifications websocket")
          (loop [msg (<! ws-channel)]
            (when-not (nil? msg)
              (println "got this notification: " msg " : " (type msg))
              (recur (<! ws-channel)))))
        (println "there was an error: " (pr-str error))))))
