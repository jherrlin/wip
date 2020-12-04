(ns client.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame.db]
   [common.events.collections :as events.collections]))




(re-frame/reg-event-db ::initialize-db
  (fn [_ _] {}))


(doseq [{:keys [n s e]} events.collections/events]
  (when s
    (re-frame.core/reg-sub n s))
  (when e
    (re-frame.core/reg-event-db n e)))
