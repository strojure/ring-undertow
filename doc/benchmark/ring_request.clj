(ns benchmark.ring-request
  (:require [immutant.web.internal.ring :as immutant]
            [immutant.web.internal.undertow]
            [ring.adapter.undertow.request :as luminus]
            [strojure.undertow-ring.impl.request :as impl]
            [strojure.undertow.server :as server]
            [strojure.zmap.core :as zmap])
  (:import (io.undertow.server HttpHandler HttpServerExchange)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare ^:private -exchange)

(defonce ^:private server! (atom nil))

(def ^:private -handler
  (reify HttpHandler
    (handleRequest [_ exchange]
      #_:clj-kondo/ignore
      (def ^HttpServerExchange -exchange exchange))))

(defn- server-start []
  (reset! server! (server/start {:port 9080 :handler -handler})))

(defn- server-stop []
  (swap! server! #(some-> % server/stop)))

(comment
  (server-start)
  ;; Execute request in ./ring_request.http
  (server-stop)
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Build request

(comment
  (impl/build-request -exchange)
  ;             Execution time mean : 864,001111 ns
  ;    Execution time std-deviation : 38,565622 ns
  ;   Execution time lower quantile : 821,576306 ns ( 2,5%)
  ;   Execution time upper quantile : 913,308299 ns (97,5%)

  (luminus/build-exchange-map -exchange)
  ;             Execution time mean : 3,671207 µs
  ;    Execution time std-deviation : 74,979837 ns
  ;   Execution time lower quantile : 3,582769 µs ( 2,5%)
  ;   Execution time upper quantile : 3,785342 µs (97,5%)

  ;; Immutant is fast but it returns java.util.Map which is converted on change.
  (immutant/ring-request-map -exchange)
  ;             Execution time mean : 678,031637 ns
  ;    Execution time std-deviation : 61,080265 ns
  ;   Execution time lower quantile : 623,968094 ns ( 2,5%)
  ;   Execution time upper quantile : 745,429229 ns (97,5%)
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Build request + add 1 key

(comment
  (-> (impl/build-request -exchange)
      (assoc :a 1))
  ;             Execution time mean : 951,598907 ns
  ;    Execution time std-deviation : 36,677844 ns
  ;   Execution time lower quantile : 897,164812 ns ( 2,5%)
  ;   Execution time upper quantile : 990,524070 ns (97,5%)

  (-> (luminus/build-exchange-map -exchange)
      (assoc :a 1))
  ;             Execution time mean : 3,892380 µs
  ;    Execution time std-deviation : 83,973248 ns
  ;   Execution time lower quantile : 3,812082 µs ( 2,5%)
  ;   Execution time upper quantile : 4,025584 µs (97,5%)

  (-> (immutant/ring-request-map -exchange)
      (assoc :a 1))
  ;             Execution time mean : 2,249681 µs
  ;    Execution time std-deviation : 102,768028 ns
  ;   Execution time lower quantile : 2,140457 µs ( 2,5%)
  ;   Execution time upper quantile : 2,414315 µs (97,5%)
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Build request + get header

(comment
  (-> (impl/build-request -exchange)
      :headers (get "cookie"))
  ;             Execution time mean : 948,206212 ns
  ;    Execution time std-deviation : 29,449050 ns
  ;   Execution time lower quantile : 919,992777 ns ( 2,5%)
  ;   Execution time upper quantile : 991,957280 ns (97,5%)

  (-> (luminus/build-exchange-map -exchange)
      :headers (get "cookie"))
  ;             Execution time mean : 3,751872 µs
  ;    Execution time std-deviation : 115,852475 ns
  ;   Execution time lower quantile : 3,661520 µs ( 2,5%)
  ;   Execution time upper quantile : 3,927895 µs (97,5%)

  (-> (immutant/ring-request-map -exchange)
      :headers (get "cookie"))
  ;             Execution time mean : 7,746406 µs
  ;    Execution time std-deviation : 246,297129 ns
  ;   Execution time lower quantile : 7,497864 µs ( 2,5%)
  ;   Execution time upper quantile : 8,014431 µs (97,5%)
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Various operations with exchange

(comment
  (.getRequestHeaders -exchange)
  ;Execution time mean : 5,305030 ns
  (.get (.getRequestHeaders -exchange) "Host")
  ;Execution time mean : 26,361309 ns
  (.getHostPort -exchange)
  ;Execution time mean : 36,396907 ns
  (.getDestinationAddress -exchange)
  ;Execution time mean : 52,344103 ns
  (.getPort (.getDestinationAddress -exchange))
  ;Execution time mean : 53,875708 ns
  (zmap/delay (.getPort (.getDestinationAddress -exchange)))
  ;Execution time mean : 30,838876 ns
  (.getHostName -exchange)
  ;Execution time mean : 30,924280 ns
  (.getHostAddress (.getAddress (.getSourceAddress -exchange)))
  ;Execution time mean : 90,639418 ns
  (.getRequestScheme -exchange)
  ;Execution time mean : 5,233583 ns
  (.getResolvedPath -exchange)
  ;Execution time mean : 5,686585 ns
  (.getRequestURI -exchange)
  ;Execution time mean : 5,320602 ns
  (.getQueryString -exchange)
  ;Execution time mean : 5,775425 ns
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
