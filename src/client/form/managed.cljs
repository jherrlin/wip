(ns client.form.managed
  (:require
   [re-frame.core :as rf]
   [client.form.compositions :as compositions]
   [client.form.events :as events]
   [clojure.spec.alpha :as s]))


(defn find-id [{:keys [id form attribute]}]
  (cond
    id id
    (and (not id) form attribute) (str (name form) "-" (name attribute))))

(defn field [{:keys [form attribute component-type spec]
              :or   {component-type :text}
              :as   m}]
  (let [on-change (if form
                    #(rf/dispatch [::events/form-value form attribute %])
                    #(rf/dispatch [::events/input-value attribute %]))
        on-focus  (if form
                    #(rf/dispatch [::events/form-visited? form attribute true])
                    #(rf/dispatch [::events/form-visited? attribute true]))
        value     (if form
                    @(rf/subscribe [::events/form-value form attribute])
                    @(rf/subscribe [::events/input-value attribute]))
        visited?  (when form
                    @(rf/subscribe [::events/form-visited? form attribute]))
        component (get {:text     compositions/text
                        :password compositions/password
                        :textarea compositions/textarea}
                       component-type)]
    [component
     (cond->
         {:id        (find-id m)
          :value     value
          :on-change on-change
          ;; :visited?  visited?
          ;; :on-focus  on-focus
          }
       spec (assoc :valid? (s/valid? spec value))
       :always (merge m))]))

(defn text-field [m]
  (field (assoc m :component-type :text)))

(defn textarea-field [m]
  (field (assoc m :component-type :textarea)))

(defn password-field [m]
  (field (assoc m :component-type :password)))
