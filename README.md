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

- The handler implementation is decoupled from [Undertow server API].

- Ring [request map]:

    - Does not contain deprecated keys.

    - `:headers` is a persistent map proxy interface over Undertow header map.
      Header names are case-insensitive. The proxy is converted to Clojure map
      on updates.

    - The request map itself is a lazy map (zmap) over `PersistentHashMap` with
      some values delayed.

    - Sessions work without additional middleware, only proper configuration of
      the Undertow server is required.

- Ring [response] allows `HttpHandler` in the `:body`, this allows to easily
  initiate processing like websocket handshake.

[Undertow server API]: https://github.com/strojure/undertow

[request map]: https://github.com/ring-clojure/ring/wiki/Concepts#requests

[response]: https://github.com/ring-clojure/ring/wiki/Concepts#responses

## Benchmarks

- [request](doc/benchmark/ring_request.clj)
- [headers](doc/benchmark/ring_request_headers.clj)
