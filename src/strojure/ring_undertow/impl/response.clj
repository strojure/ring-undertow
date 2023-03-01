(ns strojure.ring-undertow.impl.response
  "Ring response implementation."
  (:require [clojure.java.io :as io]
            [strojure.ring-undertow.impl.session :as session]
            [strojure.undertow.api.exchange :as exchange])
  (:import (clojure.lang Associative ISeq)
           (io.undertow.io Sender)
           (io.undertow.server HttpHandler HttpServerExchange)
           (java.io File InputStream OutputStream)
           (java.nio ByteBuffer)
           (java.nio.charset Charset)))

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
    (with-output-stream (fn send-file [output, _]
                          (with-open [input (io/input-stream file)]
                            (io/copy input output))))))

;; Allow to use any Undertow handler as response body.
(extend-protocol ResponseBody HttpHandler
  (send-response-fn
    [handler]
    (fn [exchange]
      (.handleRequest handler exchange))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn handle-response
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
          - `HttpHandler` The Undertow handler in the `:body` is handled, this
                          allows to initiate processing like websocket handshake.
      + Custom body types can be added by extending [[ResponseBody]] protocol.

  [1]: https://github.com/ring-clojure/ring/wiki/Concepts#responses
  "
  [^Associative response, ^HttpServerExchange exchange]
  (when response
    (when-some [headers,, (.valAt response :headers)] (doto exchange (put-headers headers)))
    (when-some [status,,, (.valAt response :status)], (doto exchange (.setStatusCode status)))
    (when-some [session (.entryAt response :session)] (doto exchange (session/put-session-entries (val session))))
    (when-some [body,,,,, (.valAt response :body)],,, (doto exchange ((send-response-fn body)))))
  nil)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
