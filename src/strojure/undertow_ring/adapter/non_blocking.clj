(ns strojure.undertow-ring.adapter.non-blocking
  (:require [strojure.undertow-ring.adapter :as adapter]
            [strojure.undertow-ring.impl.ring-request :as ring-request]
            [strojure.undertow-ring.impl.ring-response :as ring-response])
  (:import (io.undertow.server HttpHandler)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn as-non-blocking-sync-handler
  [handler]
  (vary-meta handler assoc ::adapter/handler-type ::sync-non-blocking-handler))

(defmethod adapter/handler-fn-adapter ::sync-non-blocking-handler
  [handler]
  (reify HttpHandler
    (handleRequest [this e]
      (if (and (.isInIoThread e)
               (not (.isRequestComplete e)))
        ;; Dispatch incomplete request to worker thread
        (.dispatch e this)
        ;; Execute handler on IO thread
        (-> (ring-request/build-request e)
            (handler)
            (ring-response/handle-response e))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
