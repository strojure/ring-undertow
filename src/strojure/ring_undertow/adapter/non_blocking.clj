(ns strojure.ring-undertow.adapter.non-blocking
  "Experimental adapter for synchronous non-blocking ring handler executed on IO
  thread."
  (:require [strojure.ring-undertow.adapter :as adapter]
            [strojure.ring-undertow.impl.request :as request]
            [strojure.ring-undertow.impl.response :as response])
  (:import (io.undertow.server HttpHandler)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn as-non-blocking-sync-handler
  "Adds metadata to the function `handler` to be called as non-blocking
  synchronous ring handler on IO thread."
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
        (-> (request/build-request e)
            (handler)
            (response/handle-response e))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
