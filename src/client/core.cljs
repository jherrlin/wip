(ns client.core
  (:require
   [client.router :as router]
   [client.router.events :as router.events]
   [client.websocket :as websocket]
   [re-frame.core :as re-frame]
   [client.events :as events]
   [re-frame.db]
   ["semantic-ui-react" :as semantic-ui]
   [reagent.dom :as reagent]
   [reitit.core :as r]))



(defn nav [{:keys [router current-route]}]
  [:ul
   (for [route-name (r/route-names router)
         :let       [route (r/match-by-name router route-name)
                     text (-> route :data :link-text)]]
     [:li {:key route-name :style {:display "inline" :margin-left "1em"}}
      (when (= route-name (-> current-route :data :name))
        "> ")
      ;; Create a normal links that user can click
      [:a {:href (router/href route-name)} text]])])

(comment
  (r/route-names router/router)
  )

(defn router-component [{:keys [router]}]
  (let [current-route @(re-frame/subscribe [::router.events/current-route])]
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

(defn test-ui []
  [:div
   [:> semantic-ui/Button
    {:onClick #(js/console.log "hejsan")}
    "Hejsan"]])

;; (defn ^:dev/after-load mount-root []
;;   (reagent/render [router-component {:router router/router}]
;;                   (.getElementById js/document "app")))
(defn ^:dev/after-load mount-root []
  (reagent/render [test-ui]
                  (.getElementById js/document "app")))


(defn init []
  (re-frame/clear-subscription-cache!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::router.events/initialize-router-db])
  (dev-setup)
  (router/init-routes!)
  (client.websocket/init-websocket)
  (mount-root))
