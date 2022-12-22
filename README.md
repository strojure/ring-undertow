# ring-undertow

Clojure ring adapter to Undertow web server.

[![cljdoc badge](https://cljdoc.org/badge/com.github.strojure/ring-undertow)](https://cljdoc.org/d/com.github.strojure/ring-undertow)
[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/ring-undertow.svg)](https://clojars.org/com.github.strojure/ring-undertow)

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

### Handlers

Undertow server allow to use multiple handler functions in various contexts.
This library provides functions to create server handlers from Clojure
functions. See server configuration documentation
in [Undertow server API][github_undertow].

```clojure
(ns usage.handlers
  (:require [clj-http.client :as client]
    [strojure.ring-undertow.handler :as handler]
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


;;; Direct call of handler function.

;; Synchronous ring handler - directly

(run {:handler (handler/sync-ring-handler ring-handler)})
;=> "Hello from localhost (sync)"

;; Asynchronous ring handler - directly

(run {:handler (handler/async-ring-handler ring-handler)})
;=> "Hello from localhost (async)"


;;; Using adapter configuration option.

;; Synchronous ring handler - adapter in configuration

(run {:handler ring-handler, :handler-fn-adapter handler/sync-ring-handler})
;=> "Hello from localhost (sync)"

;; Asynchronous ring handler - adapter in configuration

(run {:handler ring-handler, :handler-fn-adapter handler/async-ring-handler})
;=> "Hello from localhost (async)"


;;; Setting adapter globally before server start.

;; Synchronous ring handler - global assignment

(do (server/set-handler-fn-adapter handler/sync-ring-handler)
    (run {:handler ring-handler}))
;=> "Hello from localhost (sync)"

;; Asynchronous ring handler - global assignment

(do (server/set-handler-fn-adapter handler/async-ring-handler)
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
    {:status 404}))
```

### Request utility

There are some utility functions to get information from ring requests which is
not presented as request map keys.

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

## Compatibility reference

As far as ring handler is decoupled from server API here is a compatibility
reference for configuration options from other implementation like [luminus].

[luminus]: https://github.com/luminus-framework/ring-undertow-adapter

TODO

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
