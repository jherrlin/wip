(ns client.core
  (:require
   [client.form.events :as events]
   [client.form.inputs :as inputs]
   [client.form.managed :as managed]
   [client.websocket :as websocket]
   [clojure.spec.alpha :as s]
   [re-frame.core :as rf]
   [re-frame.db]
   [reagent.dom :as rd]
   [reagent.core :as r]
   [taoensso.sente :as sente :refer [cb-success?]]))


(rf/reg-event-db
 ::initialize-db
 (fn [_ _] {}))

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

(s/def ::non-blank-string (s/and string? (complement clojure.string/blank?)))

(defn main-panel []
  (let [clicked (r/atom false)]
    (fn []
      (let [form :login
            {:keys [password1 password2] :as form-values}
            @(rf/subscribe [::events/form-values form])]
        [:div {:style {:class   "container"
                       :display "flex"}}
         [:div {:style {:flex "1"}}

          [managed/text-field
           {:form       form
            :label      "Username"
            :attribute  :username
            :spec       ::non-blank-string
            :visited?   @clicked
            :error-text "hejsan"}]

          [managed/password-field
           {:form      form
            :attribute :password1
            :label     "Password"
            :valid?    (= password1 password2)
            :visited?  @clicked}]

          [managed/password-field
           {:form       form
            :attribute  :password2
            :label      "Password"
            :valid?     (= password1 password2)
            :visited?   @clicked
            :error-text "Passwords doesnt match"}]

          [managed/textarea-field
           {:id        "notes-id"
            :attribute :note
            :valid?    false
            :label     "Note"}]

          [inputs/button
           {:on-click (fn []
                        (js/console.log @clicked)
                        (reset! clicked true))
            :body     "Click me"}]

          [map-pre form-values]]
         [:div {:style {:flex "1"}}
          [map-pre]]]))))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (rd/render [main-panel]
             (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [::initialize-db])
  (client.websocket/init-websocket)
  (mount-root))
