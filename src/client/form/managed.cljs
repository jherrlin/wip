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

(defn field
  "Options:

  - `id`*           React id key.
  - `attribute`     What attribute the value belongs to. Namespaced keyword.
  - `error-text`*   Error text to display if error occurs.
  - `form`*         What form the attribute belongs to. Namespaced keyword. Optional.
  - `label`         Input label.
  - `spec`          Spec that will be used to validate the value.
  - `focus?`        Focus field if empty.
  - `on-focus`      What fn to run when focus is triggered.
  - `valid?`        Either bool or fn, used to validate the value.
  - `visited?`      Needs to be true in order for error text to be shown.
  - `required?`     Field is required.
  "
  [{:keys [form attribute component-type spec valid?]
    :or   {component-type :text}
    :as   m}]
  (let [on-change (if form
                    #(rf/dispatch [::events/form-value form attribute %])
                    #(rf/dispatch [::events/input-value attribute %]))
        value     (if form
                    @(rf/subscribe [::events/form-value form attribute])
                    @(rf/subscribe [::events/input-value attribute]))
        component (get {:text     compositions/text
                        :password compositions/password
                        :textarea compositions/textarea}
                       component-type)]
    [component
     (cond->
         (merge {:id        (find-id m)
                 :value     value
                 :on-change on-change}
                m)
       spec              (assoc :valid? (s/valid? spec value))
       (fn?      valid?) (assoc :valid? (valid? value))
       (boolean? valid?) (assoc :valid? valid?)

       (and (boolean? valid?) spec)
       (assoc :valid? (and (s/valid? spec value) valid?))

       (and (fn? valid?) spec)
       (assoc :valid? (and (s/valid? spec value) (valid? value))))]))

(defn text-field
  [m]
  (field (assoc m :component-type :text)))

(defn textarea-field [m]
  (field (assoc m :component-type :textarea)))

(defn password-field [m]
  (field (assoc m :component-type :password)))
