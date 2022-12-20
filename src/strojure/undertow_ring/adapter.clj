(ns strojure.undertow-ring.adapter
  (:require [strojure.undertow-ring.impl.ring-request :as ring-request]
            [strojure.undertow-ring.impl.ring-response :as ring-response]
            [strojure.undertow.api.exchange :as exchange]
            [strojure.undertow.handler :as handler]
            [strojure.undertow.server :as server])
  (:import (io.undertow.server HttpHandler)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmulti handler-fn-adapter (comp ::handler-type meta))

;; **Synchronous** handlers take one argument, a map representing a HTTP
;; request, and return a map representing the HTTP response.

(defmethod handler-fn-adapter nil
  [ring-handler]
  (handler/force-dispatch
    (reify HttpHandler
      (handleRequest [_ exchange]
        (-> (ring-request/build-request exchange)
            (ring-handler)
            (ring-response/handle-response exchange))))))

;; Handlers may also be **asynchronous**. Handlers of this type take three
;; arguments: the request map, a response callback and an exception callback.

(defmethod handler-fn-adapter ::async-handler
  [ring-handler]
  (reify HttpHandler
    (handleRequest [_ exchange]
      (exchange/async-dispatch exchange
        (ring-handler (ring-request/build-request exchange)
                      (fn handle-async [response] (ring-response/handle-response response exchange))
                      (partial exchange/async-throw exchange))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn enable-ring-handler
  []
  (server/set-handler-fn-adapter handler-fn-adapter))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn as-async-handler
  [handler]
  (vary-meta handler assoc ::handler-type ::async-handler))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
