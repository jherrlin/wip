(ns client.library.forms.user-registration
  (:require
   [client.form.events :as events]
   [client.form.inputs :as inputs]
   [client.form.managed :as managed]
   [clojure.spec.alpha :as s]
   [client.debug :as debug]
   [re-frame.core :as re-frame]
   [re-frame.db]))

(s/def ::non-blank-string (s/and string? (complement clojure.string/blank?)))

(defn user-registration-form []
  (let [form                           :login
        values                         @(re-frame/subscribe [::events/form form])
        {::keys [password1 password2]} @(re-frame/subscribe [::events/form-values form])
        clicked?                       @(re-frame/subscribe [::events/form-meta form :clicked?])
        debug-form-data?               @(re-frame/subscribe [::events/form-meta form :debug-form-data?])]
    [:div {:style {:flex "1"}}
     [managed/text-field
      {:form       form
       :label      "Username"
       :attribute  ::username
       :spec       ::non-blank-string
       :visited?   clicked?
       :focus?     true
       :required?  true
       :error-text "Username can't be empty!"}]

     [managed/password-field
      {:form      form
       :attribute ::password1
       :label     "Password"
       :spec      ::non-blank-string
       :valid?    #(= % password2)
       :required? true
       :visited?  clicked?}]

     [managed/password-field
      {:form       form
       :attribute  ::password2
       :label      "Password again"
       :spec       ::non-blank-string
       :valid?     #(= % password1)
       :visited?   clicked?
       :required?  true
       :error-text (if (empty? password2)
                     "Password can't be empty!"
                     "Passwords doesnt match")}]

     [managed/textarea-field
      {:form      form
       :attribute ::note
       :valid?    false
       :label     "Note"}]

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
       [debug/map-pre {:m  values
                       :id "user-registration-pre-data"}])]))

(defn component []
  [:div {:style {:class   "container"
                 :display "flex"}}
   [:div {:style {:flex "1"}}
    [user-registration-form]]
   [:div {:style {:flex "1"}}
    [debug/map-pre]]])
