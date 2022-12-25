(ns strojure.ring-undertow.server.luminus-adapter
  "Compatibility with the [ring-undertow-adapter][1] from Luminus framework.

  [1]: https://github.com/luminus-framework/ring-undertow-adapter
  "
  (:require [strojure.ring-undertow.server :as ring.server])
  (:import (io.undertow Undertow)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn configuration
  "Returns Undertow server configuration map for given Luminus adapter options.
  See [[run-undertow]] for the options details."
  [handler {:keys [configurator host http? port ssl-port http2?
                   io-threads worker-threads buffer-size direct-buffers?
                   async? max-entity-size
                   session-manager? custom-manager max-sessions server-name]
            :or {http? true, port 80, session-manager? true, server-name "ring-undertow"}
            :as options}]
  (cond-> {:handler (if session-manager?
                      ;; We use declarative configuration because ring handler
                      ;; adapter can be not bind at this point.
                      [{:type :strojure.undertow.handler/session
                        :session-manager (or custom-manager {:max-sessions (or max-sessions -1)
                                                             :deployment-name server-name})}
                       handler]
                      handler)}
    configurator (assoc :builder-fn-wrapper (fn [f] (fn [builder config]
                                                      (f (configurator builder) config))))
    http?
    (assoc-in [:port port] (cond-> {} host (assoc :host host)))
    ssl-port
    (assoc-in [:port ssl-port] (cond-> {:https (select-keys options [:key-managers :trust-managers :ssl-context])}
                                 host (assoc :host host)))
    http2?
    (assoc-in [:server-options :undertow/enable-http2] true)
    io-threads
    (assoc :io-threads io-threads)
    worker-threads
    (assoc :worker-threads worker-threads)
    buffer-size
    (assoc :buffer-size buffer-size)
    (some? direct-buffers?)
    (assoc :direct-buffers (boolean direct-buffers?))
    async?
    (assoc :ring-handler-type :async)
    max-entity-size
    (assoc-in [:server-options :undertow/max-entity-size] max-entity-size)))

(comment
  (configuration identity {:ssl-port 4242 :key-managers [] :trust-managers []
                           :http2? true :direct-buffers? true
                           :async? true})
  #_{:handler [{:type :strojure.undertow.handler/session,
                :session-manager {:max-sessions -1, :deployment-name nil}}
               identity],
     :port {80 {}, 4242 {:https {:https {:key-managers [], :trust-managers []}}}},
     :server-options #:undertow{:enable-http2 true},
     :direct-buffers true,
     :ring-handler-type :async}
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn run-undertow
  "Start an Undertow webserver using given handler and the supplied options:

  - `:configurator`     - a function called with the Undertow Builder instance
  - `:host`             - the hostname to listen on
  - `:http?`            - flag to enable http (defaults to true)
  - `:port`             - the port to listen on (defaults to 80)
  - `:ssl-port`         - a number, requires either `:ssl-context`, `:keystore`, or `:key-managers`
  - `:ssl-context`      - a valid `javax.net.ssl.SSLContext`
  - `:key-managers`     - a valid `javax.net.ssl.KeyManager[]`
  - `:trust-managers`   - a valid `javax.net.ssl.TrustManager[]`
  - `:http2?`           - flag to enable http2
  - `:io-threads`       - # threads handling IO, defaults to available processors
  - `:worker-threads`   - # threads invoking handlers, defaults to (* io-threads 8)
  - `:buffer-size`      - a number, defaults to 16k for modern servers
  - `:direct-buffers?`  - boolean, defaults to ~~true~~ auto (see below)
  - `:async?`           - ring async flag. When true, expect a ring async three arity handler function
  - `:max-entity-size`  - maximum size of a request entity
  - `:session-manager?` - initialize undertow session manager (default: true)
  - `:custom-manager`   - custom implementation that extends the `io.undertow.server.session.SessionManager` interface
  - `:max-sessions`     - maximum number of undertow session, for use with `InMemorySessionManager` (default: -1)
  - `:server-name`      - for use in session manager, for use with `InMemorySessionManager` (default: \"ring-undertow\")

  Not implemented options:
  - `:keystore`         - the filepath (a String) to the keystore
  - `:key-password`     - the password for the keystore
  - `:truststore`       - if separate from the keystore
  - `:trust-password`   - if `:truststore` passed
  - `:dispatch?`        - dispatch handlers off the I/O threads (default: true)
  - `:handler-proxy`    - an optional custom handler proxy function taking handler as single argument

  Not applicable options:
  - `:websocket?`       - websockets support is built-in, Luminus' response keys are not implemented.

  Difference with Luminus' adapter:
  - `:direct-buffers?`  - default value is auto, Undertow decides if true or false.

  Returns an Undertow server instance. To stop call `(.stop server)`."
  {:tag Undertow}
  [handler options]
  (-> (configuration handler options)
      (ring.server/start)
      :undertow))

(comment
  (-> (run-undertow identity {:port 8282 :ssl-port 4242
                              :http2? true :direct-buffers? true
                              :async? true})
      (.stop))
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
