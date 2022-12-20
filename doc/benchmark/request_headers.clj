(ns benchmark.request-headers
  (:require [immutant.web.internal.headers :as immutant]
            [immutant.web.internal.undertow]
            [ring.adapter.undertow.headers :as luminus]
            [strojure.undertow-ring.impl.request-headers :as impl]
            [strojure.undertow.api.exchange :as exchange])
  (:import (io.undertow.util HeaderMap)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^:private -headers-7
  "Fits to PersistentArrayMap."
  (doto (HeaderMap.)
    (exchange/put-headers!
      {"User-Agent",,,,, "Mozilla /5.0 (Windows NT 10.0 ; Win64; x64; rv:107.0) Gecko/20100101 Firefox/107.0"
       "Accept",,,,,,,,, "text/html, application/xhtml+xml, application/xml ;q=0.9,image/avif,image/webp,*/*;q=0.8"
       "Accept-Language" "ru, en ;q=0.8,de;q=0.6,uk;q=0.4,be;q=0.2"
       "Accept-Encoding" "gzip, deflate, br"
       "Connection",,,,, "keep-alive"
       "Cookie",,,,,,,,, "secret=dfe83f04-2d13-4914-88dd-5005ac317936"
       "Upgrade-Insecure-Requests" "1"})))

(def ^:private -headers-9
  "Fits to PersistentHashMap."
  (doto (HeaderMap.)
    (exchange/put-headers!
      {"User-Agent",,,,, "Mozilla /5.0 (Windows NT 10.0 ; Win64; x64; rv:107.0) Gecko/20100101 Firefox/107.0"
       "Accept",,,,,,,,, "text/html, application/xhtml+xml, application/xml ;q=0.9,image/avif,image/webp,*/*;q=0.8"
       "Accept-Language" "ru, en ;q=0.8,de;q=0.6,uk;q=0.4,be;q=0.2"
       "Accept-Encoding" "gzip, deflate, br"
       "Connection",,,,, "keep-alive"
       "Cookie",,,,,,,,, "secret=dfe83f04-2d13-4914-88dd-5005ac317936"
       "Sec-Fetch-Dest", "document"
       "Sec-Fetch-Mode", "navigate"
       "Upgrade-Insecure-Requests" "1"})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Smaller header map.

(impl/ring-headers -headers-7)
;             Execution time mean : 12,595231 ns
;    Execution time std-deviation : 0,637142 ns
;   Execution time lower quantile : 11,948979 ns ( 2,5%)
;   Execution time upper quantile : 13,504983 ns (97,5%)

(luminus/get-headers -headers-7)
;             Execution time mean : 1,745118 µs
;    Execution time std-deviation : 52,728890 ns
;   Execution time lower quantile : 1,696489 µs ( 2,5%)
;   Execution time upper quantile : 1,824060 µs (97,5%)

(immutant/headers->map -headers-7)
;             Execution time mean : 6,033873 µs
;    Execution time std-deviation : 196,252350 ns
;   Execution time lower quantile : 5,856249 µs ( 2,5%)
;   Execution time upper quantile : 6,313452 µs (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Larger header map

(impl/ring-headers -headers-9)
;             Execution time mean : 12,977983 ns
;    Execution time std-deviation : 0,432929 ns
;   Execution time lower quantile : 12,508952 ns ( 2,5%)
;   Execution time upper quantile : 13,669253 ns (97,5%)

(luminus/get-headers -headers-9)
;             Execution time mean : 3,613857 µs
;    Execution time std-deviation : 80,436241 ns
;   Execution time lower quantile : 3,523566 µs ( 2,5%)
;   Execution time upper quantile : 3,721511 µs (97,5%)

(immutant/headers->map -headers-9)
;             Execution time mean : 9,061962 µs
;    Execution time std-deviation : 242,970566 ns
;   Execution time lower quantile : 8,718457 µs ( 2,5%)
;   Execution time upper quantile : 9,332726 µs (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;;; Get header value + conversion

(-> (impl/ring-headers -headers-9)
    (get "cookie"))
;             Execution time mean : 33,047773 ns
;    Execution time std-deviation : 1,031752 ns
;   Execution time lower quantile : 31,731691 ns ( 2,5%)
;   Execution time upper quantile : 34,110913 ns (97,5%)

(-> (luminus/get-headers -headers-9)
    (get "cookie"))
;             Execution time mean : 3,780253 µs
;    Execution time std-deviation : 94,907776 ns
;   Execution time lower quantile : 3,616683 µs ( 2,5%)
;   Execution time upper quantile : 3,863265 µs (97,5%)

(-> (immutant/headers->map -headers-9)
    (get "cookie"))
;             Execution time mean : 9,266686 µs
;    Execution time std-deviation : 145,668722 ns
;   Execution time lower quantile : 9,119761 µs ( 2,5%)
;   Execution time upper quantile : 9,472851 µs (97,5%)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
