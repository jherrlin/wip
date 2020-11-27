(ns client.form.events
  (:require
   [re-frame.core :as rf]
   [clojure.spec.alpha :as s]))


(def form-events
  [{:n ::form
    :e (fn [db [_ form values]]        (assoc-in db [:form form] values))
    :s (fn [db [_ form]]                 (get-in db [:form form]))}

   {:n ::form-value
    :e (fn [db [_ form attr value]]    (assoc-in db [:form form :values attr] value))
    :s (fn [db [_ form attr]]            (get-in db [:form form :values attr]))}

   {:n ::form-values
    :e (fn [db [_ form entity]]        (assoc-in db [:form form :values] entity))
    :s (fn [db [_ form]]                 (get-in db [:form form :values]))}

   {:n ::form-original-values
    :e (fn [db [_ form values]]        (assoc-in db [:form form :original-values] values))
    :s (fn [db [_ form]]                 (get-in db [:form form :original-values]))}

   {:n ::form-visited?
    :e (fn [db [_ form attr visited?]] (assoc-in db [:form form :meta attr :visited?] visited?))
    :s (fn [db [_ form attr]]            (get-in db [:form form :meta attr :visited?]))}

   {:n ::form-meta
    :e (fn [db [_ form attr value]]    (assoc-in db [:form form :meta attr] value))
    :s (fn [db [_ form attr]]            (get-in db [:form form :meta attr]))}

   {:n ::form-changed?
    :e (fn [db [_ form]]              (= (get-in db [:form form :values])
                                         (get-in db [:form form :original-values])))}

   {:n ::form-reset
    :e (fn [db [_ form]] (update db :form dissoc form))}

   {:n ::form-reset-values
    :e (fn [db [_ form]] (update-in db [:form form] dissoc :values))}

   {:n ::form-reset-meta
    :e (fn [db [_ form]] (update-in db [:form form] dissoc :meta))}

   {:n ::form-valid?
    :s (fn [db [k entity-type]]
         (clojure.spec.alpha/valid? entity-type (get-in db [:form entity-type :values])))}])

(def input-events
  [{:n ::input-value
    :e (fn [db [_ attribute value]]  (assoc-in db [:values attribute] value))
    :s (fn [db [_ attribute]]          (get-in db [:values attribute]))}])

(doseq [{:keys [n s e]} (into form-events input-events)]
  (when s
    (re-frame.core/reg-sub n s))
  (when e
    (re-frame.core/reg-event-db n e)))
