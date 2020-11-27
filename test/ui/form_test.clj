(ns ui.form-test
  (:require
   [server.server]
   [etaoin.api :as e]
   [etaoin.keys :as k]))


(def test-state (atom {:driver nil}))

(defn start-driver []
  (swap! test-state assoc :driver (e/chrome)))

(defn stop-driver []
  (-> @test-state :driver e/quit)
  (swap! test-state assoc :driver nil))

(defn driver []
  (-> @test-state :driver))

(comment
  (start-driver)
  (stop-driver)
  (server.server/start-server)

  (e/go (driver) "http://localhost:8080")
  )
