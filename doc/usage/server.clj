(ns usage.server
  (:require [clj-http.client :as client]
            [strojure.ring-undertow.server :as server]
            [strojure.undertow.api.types :as types]))

(defn ring-handler
  "The ring handler function, both sync and async."
  ([request]
   {:body (str "Hello from " (:server-name request) " (sync)")})
  ([request respond raise]
   (future
     (try
       (respond {:body (str "Hello from " (:server-name request) " (async)")})
       (catch Throwable t
         (raise t))))))

(defn- run
  "Helper function to execute GET request, stop `server`, return response body."
  [server]
  (with-open [_ server]
    (let [port (-> server types/bean* :listenerInfo first :address :port)]
      (:body (client/get (str "http://localhost:" port "/"))))))

;; Synchronous ring handler
(run (server/start {:port 8080
                    :handler ring-handler}))
;; or
(run (server/start {:port 8080
                    :handler ring-handler
                    :ring-handler-type :sync}))

;; Asynchronous ring handler
(run (server/start {:port 8080
                    :handler ring-handler
                    :ring-handler-type :async}))
