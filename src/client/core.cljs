(ns client.core
  (:require
   [client.websocket :as websocket]
   [taoensso.sente :as sente :refer [cb-success?]]
   [re-frame.core :as rf]
   [reagent.dom :as rd]
   [taoensso.timbre :as timbre]))


(rf/reg-event-db
 ::initialize-db
 (fn [_ _] {}))

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
  (rf/clear-subscription-cache!)
  (rd/render [main-panel]
             (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [::initialize-db])
  (client.websocket/init-websocket)
  (mount-root))
