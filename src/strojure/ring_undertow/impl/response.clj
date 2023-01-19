(ns strojure.ring-undertow.impl.response
  "Ring response implementation."
  (:require [clojure.java.io :as io]
            [strojure.ring-undertow.impl.session :as session]
            [strojure.undertow.api.exchange :as exchange])
  (:import (clojure.lang IBlockingDeref IPersistentMap ISeq)
           (io.undertow.io Sender)
           (io.undertow.server HttpHandler HttpServerExchange)
           (io.undertow.server.handlers ResponseCodeHandler)
           (java.io File InputStream OutputStream)
           (java.nio ByteBuffer)
           (java.nio.charset Charset)
           (java.util.concurrent TimeoutException)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- put-headers
  [exchange headers]
  (-> (.getResponseHeaders ^HttpServerExchange exchange)
      (exchange/put-headers! headers)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defprotocol ResponseBody
  (send-response-fn
    [body]
    "Returns function `(fn [exchange])` which puts `body` in server response."))

(defn response-charset-fn
  "Returns functions without arguments which returns response `Charset`."
  [^HttpServerExchange e]
  (fn [] (Charset/forName (.getResponseCharset e))))

(defn with-response-sender
  "Returns function `(fn [exchange])` which invokes
  `(send-response response-sender charset-fn)`."
  [send-response]
  (fn [^HttpServerExchange e]
    (send-response (.getResponseSender e) (response-charset-fn e))))

(defn with-output-stream
  "Returns function `(fn [exchange])` which invokes
  `(send-response output-stream charset-fn)` dispatching to worker thread if
  necessary."
  [send-response]
  (fn wrap-exchange [^HttpServerExchange e]
    (if (.isInIoThread e)
      (.dispatch e (reify HttpHandler
                     (handleRequest [_ e] (wrap-exchange e))))
      (with-open [output (exchange/new-output-stream e)]
        (send-response output (response-charset-fn e))))))

(extend-protocol ResponseBody String
  (send-response-fn
    [string]
    (with-response-sender (fn send-string [sender, charset-fn]
                            (.send ^Sender sender string ^Charset (charset-fn))))))

(extend-protocol ResponseBody ByteBuffer
  (send-response-fn
    [buffer]
    (with-response-sender (fn send-byte-buffer [sender, _]
                            (.send ^Sender sender buffer)))))

;; InputStream - The contents of the stream is sent to the client. When the
;; stream is exhausted, the stream is closed.
(extend-protocol ResponseBody InputStream
  (send-response-fn
    [input]
    (with-output-stream (fn send-input-stream [output, _]
                          (with-open [input input]
                            (io/copy input output))))))

;; ISeq - Each element of the seq is sent to the client as a string.
(extend-protocol ResponseBody ISeq
  (send-response-fn
    [xs]
    (with-output-stream (fn send-seq [^OutputStream output, charset-fn]
                          (let [charset (charset-fn)]
                            (doseq [x xs]
                              (.write output (-> x str (.getBytes ^Charset charset)))))))))

;; File - The contents of the referenced file is sent to the client.
(extend-protocol ResponseBody File
  (send-response-fn
    [file]
    ;; TODO: Test charset for File response
    (with-output-stream (fn send-file [output, charset-fn]
                          (with-open [input (io/input-stream file)]
                            (io/copy input output))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defprotocol HandleResponse
  (handle-response
    [response exchange]
    "Handles [ring response map][1].

  - Puts `:headers` to response headers.
  - Sets `:status` if presented.
  - Updates ring session data by `:session`.
  - Sends response body.
      + Standard ring response body types:
          - `String`      The body is sent directly to the client in response
                          charset.
          - `ISeq`        Each element of the seq is sent to the client as a
                          string in response charset.
          - `File`        The contents of the referenced file is sent to the
                          client.
          - `InputStream` The contents of the stream is sent to the client. When
                          the stream is exhausted, the stream is closed.
      + Additional body types:
          - `ByteBuffer`  Similar to `String` but response charset is not used.
      + Custom body types can be added by extending [[ResponseBody]] protocol.

  Also accepts other types of `response`:

  - `nil` – empty response.
      + Responses with HTTP 404.

  - `io.undertow.server.HttpHandler` – Undertow handler.
      + Invokes `.handleRequest` on this handler.
      + This allows to initiate processing like websocket handshake.

  - `clojure.lang.IBlockingDeref` – future or promise.
      + Executes pending response asynchronously with 120 sec timeout.
      + This allows to initiate async execution from sync handler.

  Custom response types can be added by extending [[HandleResponse]] protocol.

  [1]: https://github.com/ring-clojure/ring/wiki/Concepts#responses
  "))

;; Handle Ring response map.
(extend-protocol HandleResponse IPersistentMap
  (handle-response
    [response ^HttpServerExchange exchange]
    (when-some [headers,, (.valAt response :headers)] (doto exchange (put-headers headers)))
    (when-some [status,,, (.valAt response :status)], (doto exchange (.setStatusCode status)))
    (when-some [session (.entryAt response :session)] (doto exchange (session/put-session-entries (val session))))
    (when-some [body,,,,, (.valAt response :body)],,, (doto exchange ((send-response-fn body))))
    nil))

;; Response HTTP 404 for `nil` response.
(extend-protocol HandleResponse nil
  (handle-response
    [_ exchange]
    (-> ResponseCodeHandler/HANDLE_404 (.handleRequest exchange))))

;; Allow to use any Undertow handler as response.
(extend-protocol HandleResponse HttpHandler
  (handle-response
    [handler exchange]
    (.handleRequest handler exchange)
    nil))

(extend-protocol HandleResponse IBlockingDeref
  (handle-response
    [pending exchange]
    (exchange/async-dispatch exchange
      (try
        (let [response (.deref pending 120000 ::timeout)]
          (if (identical? response ::timeout)
            (exchange/async-throw exchange (TimeoutException. "Pending response timed out"))
            (handle-response response exchange)))
        (catch Throwable throwable
          (exchange/async-throw exchange throwable))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
