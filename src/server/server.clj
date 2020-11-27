(ns server.server
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [compojure.core :refer [GET POST defroutes]]
   [compojure.route]
   [hiccup.page :refer [html5 include-js include-css]]
   [nrepl.server]
   [org.httpkit.server :as httpkit.server]
   [ring.middleware.defaults]
   [ring.middleware.keyword-params :as middleware.keyword-params]
   [ring.middleware.params :as middleware.params]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
   [taoensso.timbre :as timbre])
  (:gen-class))


(let [chsk-server
      (sente/make-channel-socket-server!
       (get-sch-adapter) {:packer :edn})
      {:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]}
      chsk-server]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv)
  (def chsk-send!                    send-fn)
  (def connected-uids                connected-uids))

(defmulti -event-msg-handler :id)

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data client-id]}]
  ;; (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  (when (not (contains? #{:client/log :chsk/ws-ping} id))  ;; this stuff will be logged anyway
    (timbre/info "incomming on ws from:" client-id id ?data))
  (future (-event-msg-handler ev-msg))) ; Handle event-msgs on a thread pool

(defmethod -event-msg-handler :default
  [{:as ev-msg :keys [event id client-id ?data ring-req ?reply-fn send-fn]}]
  (timbre/info "incomming on ws from:" client-id id ?data))

(defmethod -event-msg-handler :ws/ping
  [{:as ev-msg :keys [event id client-id ?data ring-req ?reply-fn send-fn]}]
  (timbre/info "incomming on ws from:" client-id id ?data)
  (?reply-fn "PONG!"))

(defn read-edn-file
  "Read file from filesystem and parse it to edn."
  [resource-filesystem-path]
  (try
    (edn/read-string (slurp (io/resource resource-filesystem-path)))
    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" resource-filesystem-path (.getMessage e)))
    (catch Exception e
      (printf "Error parsing edn file '%s': %s\n" resource-filesystem-path (.getMessage e)))))

(defn index-html
  "Create an index page with a CSRF token attached to it."
  [req]
  (html5
   {:style "height: 100%"}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (include-css "https://stackpath.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css")
    (include-css "//cdn.jsdelivr.net/npm/semantic-ui@2.4.1/dist/semantic.min.css")
    ;; (include-css "https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/css/bootstrap.min.css")
    ]
   [:body {:style "height: 100%"}
    [:div#app (merge
               {:data-csrf-token (:anti-forgery-token req)}
               {:style "height: 100%"})
     "loading..."]
    (->> "public/js/manifest.edn"
         (read-edn-file)
         (map :output-name)
         (mapv #(str "js/" %))
         (apply include-js))]))

(defroutes routes
  (GET "/" req (index-html req))
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (compojure.route/resources "/"))

(def handler
  (-> #'routes
      (middleware.keyword-params/wrap-keyword-params)
      (middleware.params/wrap-params)
      (ring.middleware.defaults/wrap-defaults
       ring.middleware.defaults/site-defaults)))

(defn start-server []
  (sente/start-server-chsk-router! ch-chsk event-msg-handler)
  (httpkit.server/run-server #'handler {:port 8080})
  (nrepl.server/start-server :bind "0.0.0.0" :port 8081))

(comment
  (start-server)
  )
