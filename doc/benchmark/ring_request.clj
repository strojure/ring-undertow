(ns benchmark.ring-request
  (:require [clj-http.client :as client]
            [immutant.web.internal.ring :as immutant]
            [immutant.web.internal.undertow]
            [ring.adapter.undertow.request :as luminus]
            [strojure.ring-undertow.impl.request :as impl]
            [strojure.undertow.handler :as handler]
            [strojure.undertow.server :as server]
            [strojure.zmap.core :as zmap])
  (:import (io.undertow.server HttpServerExchange)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare ^:private -exchange)

(def ^:private -handler
  (handler/with-exchange
    (fn [exchange]
      #_:clj-kondo/ignore
      (def ^HttpServerExchange -exchange exchange))))

(def ^:private -request-headers
  {"User-Agent",,,,, "Mozilla /5.0 (Windows NT 10.0 ; Win64; x64; rv:107.0) Gecko/20100101 Firefox/107.0"
   "Accept",,,,,,,,, "text/html, application/xhtml+xml, application/xml ;q=0.9,image/avif,image/webp,*/*;q=0.8"
   "Accept-Language" "ru, en ;q=0.8,de;q=0.6,uk;q=0.4,be;q=0.2"
   "Accept-Encoding" "gzip, deflate, br"
   "Connection",,,,, "keep-alive"
   "Cookie",,,,,,,,, "secret=dfe83f04-2d13-4914-88dd-5005ac317936"
   "Upgrade-Insecure-Requests" "1"})

(with-open [_ (server/start {:handler -handler :port 9080})]
  (client/get "http://localhost:9080/?param=value" {:headers -request-headers})
  (println "\nServer exchange:" -exchange "\n"))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Build request

(comment
  (impl/build-request -exchange)
  ;             Execution time mean : 697,643335 ns
  ;    Execution time std-deviation : 16,956270 ns
  ;   Execution time lower quantile : 678,883685 ns ( 2,5%)
  ;   Execution time upper quantile : 721,282042 ns (97,5%)

  (luminus/build-exchange-map -exchange)
  ;             Execution time mean : 4,317021 µs
  ;    Execution time std-deviation : 60,236093 ns
  ;   Execution time lower quantile : 4,237576 µs ( 2,5%)
  ;   Execution time upper quantile : 4,391014 µs (97,5%)

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
  ;             Execution time mean : 880,222280 ns
  ;    Execution time std-deviation : 24,823099 ns
  ;   Execution time lower quantile : 847,510288 ns ( 2,5%)
  ;   Execution time upper quantile : 907,850522 ns (97,5%)

  (-> (luminus/build-exchange-map -exchange)
      (assoc :a 1))
  ;             Execution time mean : 4,493658 µs
  ;    Execution time std-deviation : 102,608011 ns
  ;   Execution time lower quantile : 4,329457 µs ( 2,5%)
  ;   Execution time upper quantile : 4,592031 µs (97,5%)

  (-> (immutant/ring-request-map -exchange)
      (assoc :a 1))
  ;             Execution time mean : 2,304262 µs
  ;    Execution time std-deviation : 45,636796 ns
  ;   Execution time lower quantile : 2,259740 µs ( 2,5%)
  ;   Execution time upper quantile : 2,363342 µs (97,5%)
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Build request + get header

(comment
  (-> (impl/build-request -exchange)
      :headers (get "cookie"))
  ;             Execution time mean : 750,271305 ns
  ;    Execution time std-deviation : 25,946630 ns
  ;   Execution time lower quantile : 722,563834 ns ( 2,5%)
  ;   Execution time upper quantile : 785,599982 ns (97,5%)

  (-> (luminus/build-exchange-map -exchange)
      :headers (get "cookie"))
  ;             Execution time mean : 4,483495 µs
  ;    Execution time std-deviation : 82,596506 ns
  ;   Execution time lower quantile : 4,389776 µs ( 2,5%)
  ;   Execution time upper quantile : 4,581021 µs (97,5%)

  (-> (immutant/ring-request-map -exchange)
      :headers (get "cookie"))
  ;             Execution time mean : 8,169952 µs
  ;    Execution time std-deviation : 219,081729 ns
  ;   Execution time lower quantile : 7,831553 µs ( 2,5%)
  ;   Execution time upper quantile : 8,376565 µs (97,5%)
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
