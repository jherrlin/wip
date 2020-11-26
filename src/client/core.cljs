(ns client.core
  (:require
   [client.websocket :as websocket]
   [taoensso.sente :as sente :refer [cb-success?]]
   [reagent.dom :as rd]))


(defn ping-button []
  [:button {:on-click
            #(do
               (js/console.log "Sending: PING!")
               (websocket/chsk-send!
                 [:ws/ping "PING!"]
                 5000
                 (fn [cb-reply]
                   (when (cb-success? cb-reply)
                     (js/console.log "Got in reply:" cb-reply)))))}
   "Send ping!"])

(defn main-panel []
  [:div
   [:h2 "wip"]
   [ping-button]])

(defn ^:dev/after-load mount-root []
  (rd/render [main-panel]
             (.getElementById js/document "app")))

(defn init []
  (client.websocket/init-websocket)
  (mount-root))
