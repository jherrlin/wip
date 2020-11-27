(ns ui.form-test
  (:require
   [server.server]
   [etaoin.api :as e]
   [clojure.test :as t]
   [clojure.edn :as edn]))


(def test-state (atom {:driver   nil
                       :headless false}))

(defn start-driver []
  (when (-> @test-state :driver not)
    (swap! test-state assoc :driver (e/chrome))))

(defn stop-driver []
  (-> @test-state :driver e/quit)
  (swap! test-state assoc :driver nil))

(defn driver []
  (-> @test-state :driver))

(t/deftest user-registration
  (start-driver)
  (e/go (driver) "http://localhost:8080/#/library/forms/user-registration-form")
  (e/click (driver) {:tag :button :fn/text "Validate form"})
  (e/wait (driver) 1)
  (t/is (seq (e/query-all (driver) {:tag :p :fn/has-text "Username can't be empty!"})))

  (e/fill-human-el (driver) (e/query (driver) {:css "#login-username"}) "John Doe" {})

  (t/is (empty? (e/query-all (driver) {:tag :p :fn/has-text "Username can't be empty!"})))

  (t/is (seq (e/query-all (driver) {:tag :p :fn/has-text "Password can't be empty!"})))

  (e/fill-human-el (driver) (e/query (driver) {:css "#login-password1"}) "passWord" {})

  (e/fill-human-el (driver) (e/query (driver) {:css "#login-password2"}) "passWord" {})

  (t/is (empty? (e/query-all (driver) {:tag :p :fn/has-text "Password can't be empty!"})))
  (e/wait (driver) 1)
  (e/fill-el (driver) (e/query (driver) {:css "#login-note"}) "hejsan")
  (e/wait (driver) 1)
  (e/click (driver) {:tag :button :fn/text "Show debug"})

  (e/wait (driver) 1)
  (t/is (=
         (edn/read-string
          (e/get-element-inner-html-el
           (driver)
           (e/query (driver) {:css "#user-registration-pre-data"})))
         {:values
          #:client.library.forms.user-registration{:note "hejsan",
                                                   :username "John Doe",
                                                   :password1 "passWord",
                                                   :password2 "passWord"},
          :meta {:debug-form-data? true
                 :clicked? true}}))

  (e/click (driver) {:tag :button :fn/text "Reset form"})

  (t/is (empty? (e/get-element-inner-html-el
                 (driver)
                 (e/query (driver) {:css "#login-username"}))))

  (t/is (empty? (e/get-element-inner-html-el
                 (driver)
                 (e/query (driver) {:css "#login-password1"}))))

  (t/is (empty? (e/get-element-inner-html-el
                 (driver)
                 (e/query (driver) {:css "#login-password2"}))))

  (stop-driver)
  )
