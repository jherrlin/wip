(ns client.events
  (:require
   [re-frame.core :as re-frame]
   [re-frame.db]))


(re-frame/reg-event-db ::initialize-db
  (fn [_ _] {}))
