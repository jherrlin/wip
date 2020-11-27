(ns client.views
  (:require
   [client.websocket :as websocket]
   [re-frame.db]
   [client.debug :as debug]
   [taoensso.sente :as sente :refer [cb-success?]]))

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
  [:div {:style {:class   "container"
                 :display "flex"}}
   [:div {:style {:flex "1"}}
    [:h2 "WIP"]
    [ping-button]]
   [:div {:style {:flex "1"}}

    [debug/map-pre]]])

(defn test-panel []
  [:div
   [:h2 "test"]])
