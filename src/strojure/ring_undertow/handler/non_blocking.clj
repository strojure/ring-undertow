(ns strojure.ring-undertow.handler.non-blocking
  "Experimental synchronous non-blocking ring handler executed on IO thread."
  (:require [strojure.ring-undertow.impl.request :as request]
            [strojure.ring-undertow.impl.response :as response])
  (:import (io.undertow.server HttpHandler)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn sync
  "Returns HttpHandler for **synchronous** ring handler function executed *on IO
  thread*.

  The function `handler-fn` takes one argument, a map representing a HTTP
  request, and return a map representing the HTTP response."
  [handler-fn]
  (reify HttpHandler
    (handleRequest [this e]
      (if (and (.isInIoThread e)
               (not (.isRequestComplete e)))
        ;; Dispatch incomplete request to worker thread
        (.dispatch e this)
        ;; Execute handler on IO thread
        (-> (request/build-request e)
            (handler-fn)
            (response/handle-response e))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
