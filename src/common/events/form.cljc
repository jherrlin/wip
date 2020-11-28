(ns common.events.form
  (:require
   [clojure.spec.alpha :as s]))


(defn set-form [db [_ form values]]                  (assoc-in db [:form form] values))
(defn get-form [db [_ form]]                           (get-in db [:form form]))
(defn set-form-value [db [_ form attr value]]        (assoc-in db [:form form :values attr] value))
(defn get-form-value [db [_ form attr]]                (get-in db [:form form :values attr]))
(defn set-form-values [db [_ form entity]]           (assoc-in db [:form form :values] entity))
(defn get-form-values [db [_ form]]                    (get-in db [:form form :values]))
(defn set-form-original-values [db [_ form values]]  (assoc-in db [:form form :original-values] values))
(defn get-form-original-values [db [_ form]]           (get-in db [:form form :original-values]))
(defn set-form-visited? [db [_ form attr visited?]]  (assoc-in db [:form form :meta attr :visited?] visited?))
(defn get-form-visited? [db [_ form attr]]             (get-in db [:form form :meta attr :visited?]))
(defn set-form-meta [db [_ form attr value]]         (assoc-in db [:form form :meta attr] value))
(defn get-form-meta [db [_ form attr]]                 (get-in db [:form form :meta attr]))
(defn set-form-changed? [db [_ form]]               (= (get-in db [:form form :values])
                                                       (get-in db [:form form :original-values])))
(defn set-form-reset-values [db [_ form]]           (update-in db [:form form] dissoc :values))
(defn set-form-reset-meta [db [_ form]]             (update-in db [:form form] dissoc :meta))
(defn set-form-reset [db [_ form]]                      (update db :form dissoc form))
(defn get-form-valid? [db [_ form]]
  (s/valid? form (get-in db [:form form :values])))



(def form-events
  [{:n ::form
    :e set-form
    :s get-form}
   {:n ::form-value
    :e set-form-value
    :s get-form-value}
   {:n ::form-values
    :e set-form-values
    :s get-form-values}
   {:n ::form-original-values
    :e set-form-original-values
    :s get-form-original-values}
   {:n ::form-visited?
    :e set-form-visited?
    :s get-form-visited?}
   {:n ::form-meta
    :e set-form-meta
    :s get-form-meta}
   {:n ::form-changed?
    :e set-form-changed?}
   {:n ::form-reset
    :e set-form-reset}
   {:n ::form-reset-values
    :e set-form-reset-values}
   {:n ::form-reset-meta
    :e set-form-reset-meta}
   {:n ::form-valid?
    :s get-form-valid?}])
