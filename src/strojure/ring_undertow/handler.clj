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
  "Returns HttpHandler for **synchronous** [ring handler function][1].

  The function `handler-fn` takes one argument, a map representing a HTTP
  request, and return a map representing the HTTP response.

  [1]: https://github.com/ring-clojure/ring/wiki/Concepts#handlers
  "
  [handler-fn]
  (handler/dispatch
    (reify HttpHandler
      (handleRequest [_ exchange]
        (-> (request/build-request exchange)
            (handler-fn)
            (response/handle-response exchange))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn ring-async
  "Returns HttpHandler for **asynchronous** [ring handler function][1].

  The function `handler-fn` takes three arguments: the request map, a response
  callback and an exception callback.

  [1]: https://github.com/ring-clojure/ring/wiki/Concepts#handlers
  "
  [handler-fn]
  (reify HttpHandler
    (handleRequest [_ exchange]
      (exchange/async-dispatch exchange
        (handler-fn (request/build-request exchange)
                    (fn handle-async [response] (response/handle-response response exchange))
                    (partial exchange/async-throw exchange))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn ring
  "Returns HttpHandler for [ring handler function][1].

  - `:fn`    The function to return `HttpHandler` for.
  - `:async` Boolean flag if handler function is synchronous or asynchronous.
             See also documentation for [[ring-sync]] and [[ring-async]].

  Can be used declaratively in server-configuration:

      (server/start {:handler {:type ring, :fn my-handler-fn, :async true}})

  NOTE: The `strojure.ring-undertow.handler` namespace need to be imported to
        make declarative configuration available.

  [1]: https://github.com/ring-clojure/ring/wiki/Concepts#handlers
  "
  [{handler :fn async :async}]
  (assert (fn? handler) (str "Requires function in :handler key: " (pr-str handler)))
  (if async
    (ring-async handler)
    (ring-sync handler)))

(handler/define-type ring {:alias ::ring :as-handler ring})

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
