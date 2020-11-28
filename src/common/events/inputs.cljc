(ns common.events.inputs)


(defn set-input-value [db [_ attribute value]]  (assoc-in db [:values attribute] value))
(defn get-input-value [db [_ attribute]]          (get-in db [:values attribute]))

(def input-events
  [{:n ::input-value
    :e set-input-value
    :s get-input-value}])
