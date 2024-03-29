# ring-undertow

Clojure ring adapter to Undertow web server.

[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/ring-undertow.svg)](https://clojars.org/com.github.strojure/ring-undertow)

[![cljdoc badge](https://cljdoc.org/badge/com.github.strojure/ring-undertow)](https://cljdoc.org/d/com.github.strojure/ring-undertow)
[![tests](https://github.com/strojure/ring-undertow/actions/workflows/tests.yml/badge.svg)](https://github.com/strojure/ring-undertow/actions/workflows/tests.yml)

## Motivation

- Performant implementation of the Ring handler concept.
- Lazy evaluation of request keys which are slow to compute but probably never
  used.
- Adopt features provided by underlying Undertow server, like sessions,
  websockets or multiple handlers on single server instance.

## Features

- The handler implementation is decoupled
  from [Undertow server API][github_undertow].

- Ring [request map][ring_requests]:

    - Does not contain deprecated keys.

    - `:headers` is a persistent map proxy interface over Undertow header map.
      Header names are case-insensitive. The proxy is converted to Clojure map
      on updates.

    - The request map itself is a [lazy map][github_zmap] with some values
      delayed.

    - Sessions work without additional middleware, only proper configuration of
      the Undertow server is required.

    - Does not contain rarely used keys like `:websocket?`. But this data can be
      got from request using functions from the `strojure.ring-undertow.request`
      namespace.

- Ring [response][ring_response] allows `HttpHandler` in the `:body`, this
  allows to easily initiate processing like websocket handshake.

## Usage

### Server

The library provides its own [functions][cljdoc_server] to start and stop
server. The [start][cljdoc_server_start] function enables ring handler adapter
(sync or async) for all functions specified as handlers in server configuration.

```clojure
(ns usage.server
  (:require [java-http-clj.core :as http]
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
      (:body (http/send {:uri (str "http://localhost:" port "/")})))))

;; Synchronous ring handler
(run (server/start {:port 0
                    :handler ring-handler}))
;; or
(run (server/start {:port 0
                    :handler ring-handler
                    :ring-handler-type :sync}))

;; Asynchronous ring handler
(run (server/start {:port 0
                    :handler ring-handler
                    :ring-handler-type :async}))
```

### Handlers

Undertow server allow to use multiple handler functions in various contexts.
This library provides [functions to create server handlers][cljdoc_handler] from
Clojure functions. For server configuration documentation
see [Undertow server API][github_undertow].

```clojure
(ns usage.handlers
  (:require [java-http-clj.core :as http]
            [strojure.ring-undertow.handler :as ring.handler]
            [strojure.undertow.api.types :as types]
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
  (with-open [server (server/start (assoc config :port 0))]
    (:body (http/send {:uri (str "http://localhost:"
                                 (-> server types/bean* :listenerInfo first :address :port)
                                 "/")}))))

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
(run {:handler {:type ring.handler/ring :ring-handler ring-handler}})
;=> "Hello from localhost (sync)"

;; Asynchronous ring handler - declarative
(run {:handler {:type ::ring.handler/ring :ring-handler ring-handler
                :ring-handler-type :async}})
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
```

### Websockets

Websocket server handler can be setup without involving ring handlers like
described in [Undertow server API][github_undertow].

But it is also possible to return websocket handler in response `:body` to
establish websocket connection.

```clojure
(ns usage.websockets
  (:require
    [strojure.ring-undertow.request :as request]
    [strojure.undertow.handler :as handler]
    [strojure.undertow.websocket.channel :as channel]))

(defn on-message
  "Websocket callback."
  [{:keys [channel text]}]
  (channel/send-text (str "Received: " text) channel nil))

(defn ring-handler
  "Ring handler initiating websocket connection."
  [request]
  (if (request/websocket? request)
    {:body (handler/websocket {:on-message on-message})}
    {:status 400}))
```

### Request utility

There are [some utility functions][cljdoc_request] to get information from ring
requests which is not presented as request map keys.

```clojure
(ns usage.request
  (:require [strojure.ring-undertow.request :as request]))

;; Check if sessions are enabled in Undertow server configuration.

(request/sessions-enabled? {})
;=> nil

;; Check if request is websocket upgrade request.

(request/websocket? {})
;=> nil

(request/websocket? {:headers {"upgrade" "unknown"}})
;=> false

(request/websocket? {:headers {"upgrade" "websocket"}})
;=> true
```

## Compatibility

There are another implementations of ring adapter for Undertow. This library
provides “adapters for adapters” for some of them:

- The [server.luminus-adapter][cljdoc_server_luminus] namespace for the
  [ring-undertow-adapter][github_luminus] in the Luminus framework.

## Benchmarks

- [request](doc/benchmark/ring_request.clj)
- [headers](doc/benchmark/ring_request_headers.clj)

---

[github_undertow]:
https://github.com/strojure/undertow

[ring_requests]:
https://github.com/ring-clojure/ring/wiki/Concepts#requests

[ring_response]:
https://github.com/ring-clojure/ring/wiki/Concepts#responses

[github_zmap]:
https://github.com/strojure/zmap

[github_luminus]:
https://github.com/luminus-framework/ring-undertow-adapter

[cljdoc_server]:
https://cljdoc.org/d/com.github.strojure/ring-undertow/CURRENT/api/strojure.ring-undertow.server

[cljdoc_server_start]:
https://cljdoc.org/d/com.github.strojure/ring-undertow/CURRENT/api/strojure.ring-undertow.server#start

[cljdoc_request]:
https://cljdoc.org/d/com.github.strojure/ring-undertow/CURRENT/api/strojure.ring-undertow.request

[cljdoc_handler]:
https://cljdoc.org/d/com.github.strojure/ring-undertow/CURRENT/api/strojure.ring-undertow.handler

[cljdoc_server_luminus]:
https://cljdoc.org/d/com.github.strojure/ring-undertow/CURRENT/api/strojure.ring-undertow.server.luminus-adapter

[handler/dispatch]:
https://cljdoc.org/d/com.github.strojure/undertow/CURRENT/api/strojure.undertow.handler#dispatch

[handler/session]:
https://cljdoc.org/d/com.github.strojure/undertow/CURRENT/api/strojure.undertow.handler#session
