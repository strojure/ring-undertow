(ns usage.handlers
  (:require [clj-http.client :as client]
            [strojure.ring-undertow.handler :as ring.handler]
            [strojure.undertow.server :as server]))

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
  "Helper function to start server with `config`, execute HTTP request, stop
  server, return response body."
  [config]
  (with-open [_ (server/start (assoc config :port 8080))]
    (:body (client/get "http://localhost:8080/"))))

;; ## Explicit invocation of handler function

;; Synchronous ring handler - explicitly
(run {:handler (ring.handler/sync ring-handler)})
;=> "Hello from localhost (sync)"

;; Asynchronous ring handler - explicitly
(run {:handler (ring.handler/async ring-handler)})
;=> "Hello from localhost (async)"

;; ## Using adapter configuration option

;; Synchronous ring handler - adapter in configuration
(run {:handler ring-handler, :handler-fn-adapter ring.handler/sync})
;=> "Hello from localhost (sync)"

;; Asynchronous ring handler - adapter in configuration
(run {:handler ring-handler, :handler-fn-adapter ring.handler/async})
;=> "Hello from localhost (async)"

;; ## Using declarative handler configuration
;;
;; NOTE: The `strojure.ring-undertow.handler` namespace need to be imported.

;; Synchronous ring handler - declarative
(run {:handler {:type ring.handler/ring :fn ring-handler}})
;=> "Hello from localhost (sync)"

;; Asynchronous ring handler - declarative
(run {:handler {:type ::ring.handler/ring :fn ring-handler :async true}})
;=> "Hello from localhost (async)"

;; ## Setting adapter globally before server start

;; Synchronous ring handler - global assignment
(do (server/set-handler-fn-adapter ring.handler/sync)
    (run {:handler ring-handler}))
;=> "Hello from localhost (sync)"

;; Asynchronous ring handler - global assignment
(do (server/set-handler-fn-adapter ring.handler/async)
    (run {:handler ring-handler}))
;=> "Hello from localhost (async)"
