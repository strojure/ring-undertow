(ns strojure.ring-undertow.handler
  "Ring handler for Undertow server."
  (:refer-clojure :exclude [sync])
  (:require [strojure.ring-undertow.impl.request :as request]
            [strojure.ring-undertow.impl.response :as response]
            [strojure.undertow.api.exchange :as exchange]
            [strojure.undertow.handler :as handler])
  (:import (clojure.lang MultiFn)
           (io.undertow.server HttpHandler)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn sync
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

(defn async
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

(defmulti adapter
  "Returns handler function adapter for `:ring-handler-type` key in server
  configuration."
  {:arglists '([config])}
  :ring-handler-type)

(.addMethod ^MultiFn adapter nil (constantly sync))
(.addMethod ^MultiFn adapter :sync (constantly sync))
(.addMethod ^MultiFn adapter :async (constantly async))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn ring
  "Returns HttpHandler for [ring handler function][1].

  - `:ring-handler` The function to return `HttpHandler` for.

  - `:ring-handler-type` The type of all ring handlers in the configuration.
      - No value or value `:sync` means synchronous ring handler [[sync]].
      - Value `:async` means asynchronous ring handler [[async]].

  Can be used declaratively in server-configuration:

      (server/start {:handler {:type ring, :ring-handler my-handler-fn}})

      (server/start {:handler {:type ring, :ring-handler my-handler-fn,
                               :ring-handler-type :async}})

  NOTE: The `strojure.ring-undertow.handler` namespace need to be imported to
        make declarative configuration available.

  [1]: https://github.com/ring-clojure/ring/wiki/Concepts#handlers
  "
  [{:keys [ring-handler] :as config}]
  (assert (fn? ring-handler)
          (str "Requires function in :handler key: " (pr-str ring-handler)))
  ((adapter config) ring-handler))

(handler/define-type ring {:alias ::ring :as-handler ring})

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
