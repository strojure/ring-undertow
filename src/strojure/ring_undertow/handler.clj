(ns strojure.ring-undertow.handler
  "Ring handler for Undertow server."
  (:require [strojure.ring-undertow.impl.request :as request]
            [strojure.ring-undertow.impl.response :as response]
            [strojure.undertow.api.exchange :as exchange]
            [strojure.undertow.handler :as handler])
  (:import (io.undertow.server HttpHandler)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn ring-sync
  "Returns HttpHandler for **synchronous** ring handler function.

  The function `handler-fn` takes one argument, a map representing a HTTP
  request, and return a map representing the HTTP response."
  [handler-fn]
  (handler/dispatch
    (reify HttpHandler
      (handleRequest [_ exchange]
        (-> (request/build-request exchange)
            (handler-fn)
            (response/handle-response exchange))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn ring-async
  "Returns HttpHandler for **asynchronous** ring handler function.

  The function `handler-fn` takes three arguments: the request map, a response
  callback and an exception callback."
  [handler-fn]
  (reify HttpHandler
    (handleRequest [_ exchange]
      (exchange/async-dispatch exchange
        (handler-fn (request/build-request exchange)
                    (fn handle-async [response] (response/handle-response response exchange))
                    (partial exchange/async-throw exchange))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
