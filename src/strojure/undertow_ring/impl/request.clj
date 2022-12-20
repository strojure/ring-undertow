(ns strojure.undertow-ring.impl.request
  "Ring request implementation."
  (:require [strojure.undertow-ring.impl.headers :as headers]
            [strojure.undertow-ring.impl.session :as session]
            [strojure.undertow.api.exchange :as exchange]
            [strojure.zmap.core :as zmap])
  (:import (clojure.lang PersistentHashMap)
           (io.undertow.server HttpServerExchange)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- method-keyword
  [s]
  (case s "GET" :get "POST" :post "PUT" :put "DELETE" :delete "HEAD" :head "OPTIONS" :options
          (zmap/delay (keyword (.toLowerCase ^String s)))))

(comment
  (method-keyword "GET")
  #_=> :get
  ;             Execution time mean : 3,771426 ns
  ;    Execution time std-deviation : 0,156418 ns
  ;   Execution time lower quantile : 3,602781 ns ( 2,5%)
  ;   Execution time upper quantile : 3,935679 ns (97,5%)
  (method-keyword "BOOM")
  ;=> #object[strojure.zmap.impl.core.BoxedDelay 0x7715336a {:status :pending, :val nil}]
  ;             Execution time mean : 46,856256 ns
  ;    Execution time std-deviation : 1,013279 ns
  ;   Execution time lower quantile : 45,452867 ns ( 2,5%)
  ;   Execution time upper quantile : 48,007985 ns (97,5%)
  (keyword (.toLowerCase "BOOM"))
  ;             Execution time mean : 53,450683 ns
  ;    Execution time std-deviation : 15,116514 ns
  ;   Execution time lower quantile : 43,600598 ns ( 2,5%)
  ;   Execution time upper quantile : 79,003333 ns (97,5%)
  )

(defn- scheme-keyword
  [s]
  (case s "http" :http "https" :https
          (keyword (.toLowerCase ^String s))))

(comment
  (scheme-keyword "http")
  #_=> :http
  ;             Execution time mean : 2,649399 ns
  ;    Execution time std-deviation : 1,004296 ns
  ;   Execution time lower quantile : 1,863488 ns ( 2,5%)
  ;   Execution time upper quantile : 4,057465 ns (97,5%)
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn build-request
  "Returns [ring request map][1] for given server exchange.

  - Does not contain deprecated keys.

  - The `:headers` is a persistent map proxy interface over Undertow header map.
    Header names are case-insensitive. The proxy is converted to Clojure map on
    updates.

  - The request map itself is a lazy map (zmap) over `PersistentHashMap` with
    some values delayed.

  - The `:session` key is provided when session data is not empty. No additional
    middleware is required for sessions, but only proper configuration of the
    Undertow server.

  - The `:body` is only provided in incomplete request with not consumed input
    stream.

  [1]: https://github.com/ring-clojure/ring/wiki/Concepts#requests
  "
  [^HttpServerExchange e]
  (let [query-string (.getQueryString e)
        query-string (when-not (.isEmpty query-string) query-string)
        context (.getResolvedPath e)
        context (when-not (.isEmpty context) context)
        session (session/get-session e)
        body (exchange/get-input-stream e)]
    (-> (.asTransient PersistentHashMap/EMPTY)
        ;; TODO: Decide about keyword for server exchange
        (.assoc :undertow/exchange e)
        (.assoc :server-port,,, (zmap/delay (.getPort (.getDestinationAddress e))))
        (.assoc :server-name,,, (zmap/delay (.getHostName e)))
        (.assoc :remote-addr,,, (zmap/delay (.getHostAddress (.getAddress (.getSourceAddress e)))))
        (.assoc :uri,,,,,,,,,,, (.getRequestURI e))
        (.assoc :scheme,,,,,,,, (scheme-keyword (.getRequestScheme e)))
        (.assoc :request-method (method-keyword (.toString (.getRequestMethod e))))
        (.assoc :headers,,,,,,, (headers/header-map-proxy (.getRequestHeaders e)))
        (cond->
          query-string,,, (.assoc :query-string query-string)
          context,,,,,,,, (.assoc :context context)
          session,,,,,,,, (.assoc :session session)
          body,,,,,,,,,,, (.assoc :body body))
        (.persistent)
        (zmap/wrap))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
