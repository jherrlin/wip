(ns client.library.forms.login
  (:require
   [client.form.events :as events]
   [client.form.inputs :as inputs]
   [client.form.managed :as managed]
   [clojure.spec.alpha :as s]
   [client.debug :as debug]
   [re-frame.core :as re-frame]
   [re-frame.db]))


(s/def ::non-blank-string (s/and string? (complement clojure.string/blank?)))

(defn login-form []
  (let [form             :user-registration
        form-values      @(re-frame/subscribe [::events/form-values form])
        clicked?         @(re-frame/subscribe [::events/form-meta form :clicked?])
        debug-form-data? @(re-frame/subscribe [::events/form-meta form :debug-form-data?])]
    [:div {:style {:flex "1"}}
     [managed/text-field
      {:form       form
       :label      "Username"
       :attribute  ::username
       :spec       ::non-blank-string
       :visited?   clicked?
       :focus?     true
       :error-text "hejsan"}]

     [managed/password-field
      {:form      form
       :attribute ::password
       :label     "Password"
       :spec      ::non-blank-string
       :visited?  clicked?}]

     [inputs/button
      {:on-click #(re-frame/dispatch [::events/form-meta form :debug-form-data? (not debug-form-data?)])
       :body     (if debug-form-data? "Hide debug" "Show debug")}]

     [inputs/button
      {:on-click #(re-frame/dispatch [::events/form-meta form :clicked? true])
       :body     "Validate form"}]

     [inputs/button
      {:on-click #(re-frame/dispatch [::events/form-reset form ])
       :body     "Reset form"}]

     (when debug-form-data?
       [debug/map-pre {:m form-values}])]))

(defn component []
  [:div {:style {:class   "container"
                 :display "flex"}}
   [:div {:style {:flex "1"}}
    [login-form]]
   [:div {:style {:flex "1"}}
    [debug/map-pre]]])
