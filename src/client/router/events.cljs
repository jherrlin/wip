(ns client.router.events
  (:require
   [re-frame.core :as re-frame]
   [reitit.frontend.controllers :as rfc]
   [reitit.frontend.easy :as rfe]))


(re-frame/reg-event-db
 ::initialize-router-db
 (fn [db [_]]
   (assoc db :current-route nil)))

(re-frame/reg-event-fx ::push-state
  (fn [db [_ & route]]
    {:push-state route}))

(re-frame/reg-event-db ::navigated
  (fn [db [_ new-match]]
    (let [old-match   (:current-route db)
          controllers (rfc/apply-controllers (:controllers old-match) new-match)]
      (assoc db :current-route (assoc new-match :controllers controllers)))))

(re-frame/reg-sub ::current-route
  (fn [db]
    (:current-route db)))

(re-frame/reg-fx :push-state
  (fn [route]
    (apply rfe/push-state route)))
