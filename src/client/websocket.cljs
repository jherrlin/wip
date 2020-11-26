(ns client.websocket
  (:require
   [taoensso.sente :as sente :refer [cb-success?]]
   [taoensso.timbre :as timbre]))


(declare chsk-send!)

(defmulti -event-msg-handler :id)

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data]}]
  (timbre/info "incomming on ws from server" ev-msg)
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :default
  [{:as ev-msg :keys [event ?data]}]
  (timbre/info "on default event handler" ev-msg)
  nil)

(defn init-websocket []
  (let [?csrf-token (when-let [el (.getElementById js/document "app")]
                      (.getAttribute el "data-csrf-token"))]
    (if (not ?csrf-token)
      (timbre/error "CSRF token not found or session-id not set. Websocket wont start.")
      (try
        (let [{:keys [chsk ch-recv send-fn state]}
              (sente/make-channel-socket! "/chsk"
                                          ?csrf-token
                                          {:type           :auto
                                           :wrap-recv-evs? false
                                           :client-id      ?csrf-token})]

          (def chsk       chsk)
          (def ch-chsk    ch-recv)     ; ChannelSocket's receive channel
          (def chsk-send! send-fn)
          (def chsk-state state)       ; Watchable, read-only atom

          (sente/start-client-chsk-router!
           ch-chsk event-msg-handler))

        (catch js/Error e
          (js/console.log e))))))
