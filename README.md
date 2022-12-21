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

## Benchmarks

- [request](doc/benchmark/ring_request.clj)
- [headers](doc/benchmark/ring_request_headers.clj)
