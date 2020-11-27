(ns client.core
  (:require
   [client.form.events :as events]
   [client.form.inputs :as inputs]
   [client.form.managed :as managed]
   [client.websocket :as websocket]
   [clojure.spec.alpha :as s]
   [re-frame.core :as re-frame]
   [re-frame.db]
   [reagent.dom :as reagent]
   [taoensso.sente :as sente :refer [cb-success?]]

     [reitit.core :as r]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]
   ))


(re-frame/reg-event-db ::initialize-db
  (fn [db _]
    (if db
      db
      {:current-route nil})))

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

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))

(def routes
  ["/"
   [""
    {:name      ::home
     :view      main-panel
     :link-text "Home"
     :controllers
     [{;; Do whatever initialization needed for home page
       ;; I.e (re-frame/dispatch [::events/load-something-with-ajax])
       :start (fn [& params](js/console.log "Entering home page"))
       ;; Teardown can be done here.
       :stop  (fn [& params] (js/console.log "Leaving home page"))}]}]
   ["sub-page1"
    {:name      ::sub-page1
     :view      test-panel
     :link-text "Sub page 1"
     :controllers
     [{:start (fn [& params] (js/console.log "Entering sub-page 1"))
       :stop  (fn [& params] (js/console.log "Leaving sub-page 1"))}]}]])

(defn on-navigate [new-match]
  (when new-match
    (re-frame/dispatch [::navigated new-match])))

(def router
  (rf/router
    routes
    {:data {:coercion rss/coercion}}))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
    router
    on-navigate
    {:use-fragment true}))

(defn nav [{:keys [router current-route]}]
  [:ul
   (for [route-name (r/route-names router)
         :let       [route (r/match-by-name router route-name)
                     text (-> route :data :link-text)]]
     [:li {:key route-name}
      (when (= route-name (-> current-route :data :name))
        "> ")
      ;; Create a normal links that user can click
      [:a {:href (href route-name)} text]])])

(defn router-component [{:keys [router]}]
  (let [current-route @(re-frame/subscribe [::current-route])]
    [:div
     [nav {:router router :current-route current-route}]
     (when current-route
       [(-> current-route :data :view)])]))

;;; Setup ;;;

(def debug? ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))


(defn init []
  (re-frame/clear-subscription-cache!)
  (re-frame/dispatch-sync [::initialize-db])
  (dev-setup)
  (init-routes!)
  (client.websocket/init-websocket)
  (reagent/render [router-component {:router router}]
                  (.getElementById js/document "app")))
