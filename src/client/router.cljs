(ns client.router
  (:require
   [re-frame.core :as re-frame]
   [client.router.events :as router.events]
   [reitit.coercion.spec :as rss]
   [reitit.frontend :as rf]
   [client.views :as views]
   [client.library.forms.login :as library.forms.login]
   [client.library.forms.user-registration :as library.forms.user-registration]
   [reitit.frontend.easy :as rfe]))


(def routes
  ["/"
   ["" {:name      ::home
        :view      views/main-panel
        :link-text "Home"
        :controllers
        [{;; Do whatever initialization needed for home page
          ;; I.e (re-frame/dispatch [::events/load-something-with-ajax])
          :start (fn [& params] (js/console.log "Entering home page"))
          ;; Teardown can be done here.
          :stop  (fn [& params]
                   (js/console.log params)
                   (js/console.log "Leaving home page"))}]}]
   ["library/"
    ["forms/"
     ["login-form"
      {:name      :library.forms/login-form
       :view      library.forms.login/component
       :link-text "Login form"
       :controllers
       [{:start (fn [& params] (js/console.log "Entering sub-page 1"))
         :stop  (fn [& params] (js/console.log "Leaving sub-page 1"))}]}]

     ["user-registration-form"
      {:name      :library.forms/user-registration-form
       :view      library.forms.user-registration/component
       :link-text "User registration"
       :controllers
       [{:start (fn [& params] (js/console.log "Entering sub-page 1"))
         :stop  (fn [& params] (js/console.log "Leaving sub-page 1"))}]}]
     ]]])

(def router
  (rf/router
    routes
    {:data {:coercion rss/coercion}}))

(defn on-navigate [new-match]
  (when new-match
    (re-frame/dispatch [::router.events/navigated new-match])))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
    router
    on-navigate
    {:use-fragment true}))

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))
