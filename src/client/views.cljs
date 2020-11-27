(ns client.views
  (:require
   [client.form.events :as events]
   [client.form.inputs :as inputs]
   [client.form.managed :as managed]
   [client.websocket :as websocket]
   [clojure.spec.alpha :as s]
   [re-frame.core :as re-frame]
   [re-frame.db]
   [taoensso.sente :as sente :refer [cb-success?]]
   ))

(s/def ::non-blank-string (s/and string? (complement clojure.string/blank?)))

(defn map-pre [& [m]]
  [:div {:style {:width "100%"}}
   [:pre (with-out-str (cljs.pprint/pprint (or m @re-frame.db/app-db)))]])

(defn ping-button []
  [:button {:on-click
            #(do
               (js/console.log "Sending: PING!")
               (websocket/chsk-send!
                 [:ws/ping "PING!"]
                 5000
                 (fn [cb-reply]
                   (when (cb-success? cb-reply)
                     (js/console.log "Got in reply:" cb-reply)))))}
   "Send ping!"])

(defn form-panel []
  (let [form                                           :login
        {::keys [password1 password2] :as form-values} @(re-frame/subscribe [::events/form-values form])
        clicked?                                       @(re-frame/subscribe [::events/form-meta form :clicked?])
        debug-form-data?                               @(re-frame/subscribe [::events/form-meta form :debug-form-data?])]
    [:div {:style {:flex "1"}}
     [managed/text-field
      {:form       form
       :label      "Username"
       :attribute  ::username
       :spec       ::non-blank-string
       :visited?   clicked?
       :focus?     true
       :required?  true
       :error-text "hejsan"}]

     [managed/password-field
      {:form      form
       :attribute ::password1
       :label     "Password"
       :spec      ::non-blank-string
       :valid?    #(= % password2)
       :visited?  clicked?}]

     [managed/password-field
      {:form       form
       :attribute  ::password2
       :label      "Password"
       :spec       ::non-blank-string
       :valid?     #(= % password1)
       :visited?   clicked?
       :error-text "Passwords doesnt match"}]

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
       [map-pre form-values])]))

(defn main-panel []
  [:div {:style {:class   "container"
                 :display "flex"}}
   [:div {:style {:flex "1"}}
    [form-panel]]
   [:div {:style {:flex "1"}}
    [map-pre]]])

(defn test-panel []
  [:div
   [:h2 "test"]])
