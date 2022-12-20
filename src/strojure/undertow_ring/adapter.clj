(ns strojure.undertow-ring.adapter
  "Ring adapter to coerce Clojure functions to Undertow server handlers."
  (:require [strojure.undertow-ring.impl.request :as request]
            [strojure.undertow-ring.impl.response :as response]
            [strojure.undertow.api.exchange :as exchange]
            [strojure.undertow.handler :as handler]
            [strojure.undertow.server :as server])
  (:import (io.undertow.server HttpHandler)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmulti handler-fn-adapter
  "The function `(fn [f] handler)` to coerce ring handler to the instance of
  `HttpHandler`. To be set using `server/set-handler-fn-adapter` or passed as
  `::server/handler-fn-adapter` configuration option."
  {:arglists '([ring-handler])}
  (comp ::handler-type meta))

;; **Synchronous** handlers take one argument, a map representing a HTTP
;; request, and return a map representing the HTTP response.

(defmethod handler-fn-adapter nil
  [ring-handler]
  (handler/force-dispatch
    (reify HttpHandler
      (handleRequest [_ exchange]
        (-> (request/build-request exchange)
            (ring-handler)
            (response/handle-response exchange))))))

;; Handlers may also be **asynchronous**. Handlers of this type take three
;; arguments: the request map, a response callback and an exception callback.

(defmethod handler-fn-adapter ::async-handler
  [ring-handler]
  (reify HttpHandler
    (handleRequest [_ exchange]
      (exchange/async-dispatch exchange
        (ring-handler (request/build-request exchange)
                      (fn handle-async [response] (response/handle-response response exchange))
                      (partial exchange/async-throw exchange))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn enable-ring-handler
  "Permanently sets server's adapter to the [[handler-fn-adapter]]."
  []
  (server/set-handler-fn-adapter handler-fn-adapter))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn as-async-handler
  "Adds metadata to the function `handler` to be called as asynchronous ring
  handler."
  [handler]
  (vary-meta handler assoc ::handler-type ::async-handler))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
