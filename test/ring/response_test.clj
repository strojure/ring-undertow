(ns ring.response-test
  (:require [clojure.java.io :as io]
            [clojure.test :as test :refer [deftest testing]]
            [java-http-clj.core :as http]
            [strojure.ring-undertow.server :as server]
            [strojure.undertow.api.types :as types]
            [strojure.undertow.handler :as handler])
  (:import (io.undertow.server HttpHandler)
           (java.io ByteArrayInputStream)
           (java.nio ByteBuffer)))

(set! *warn-on-reflection* true)

(declare thrown?)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- t
  "Executes HTTP request and returns HTTP response."
  [{:keys [handler, response, method, uri, headers, body, wrap-handler]
    :or {response {}, method :get, uri "/", headers {}}}]
  (with-open [server (server/start {:handler (cond-> (or handler (constantly response))
                                               (vector? wrap-handler) (->> (conj wrap-handler)))
                                    :port 0})]
    (http/send {:method method
                :uri (str "http://localhost:"
                          (-> server types/bean* :listenerInfo first :address :port)
                          uri)
                :headers headers
                :body body})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest response-status-t

  (test/is (= 200
              (:status (t {}))))

  (test/is (= 200
              (:status (t {:response {:status 200}}))))

  (test/is (= 404
              (:status (t {:response {:status 404}}))))

  (test/is (= 500
              (:status (t {:response {:status 500}}))))

  )

(deftest response-body-t

  (testing "String"

    (test/is (= "body"
                (:body (t {:response {:body "body"}}))))

    )

  (testing "InputStream - The contents of the stream is sent to the client."

    (testing "UTF-8 charset"

      (test/is (= "body"
                  (:body (t {:response {:body (-> "body" (.getBytes "UTF-8") (ByteArrayInputStream.))}}))))

      (test/is (= "привет"
                  (:body (t {:response {:body (-> "привет" (.getBytes "UTF-8") (ByteArrayInputStream.))}}))))

      )

    (testing "Not UTF-8 charset"

      (test/is (= "привет"
                  (:body (t {:response {:body (-> "привет" (.getBytes "Windows-1251") (ByteArrayInputStream.))
                                        :headers {"Content-Type" "text/plain; charset=Windows-1251"}}}))))

      )

    )

  (testing "ISeq - Each element of the seq is sent to the client as a string."

    (test/is (= "body"
                (:body (t {:response {:body (seq ["b" "o" "d" "y"])}}))))

    (test/is (= "body"
                (:body (t {:response {:body (seq [\b \o \d \y])}}))))

    )

  (testing "File - The contents of the referenced file is sent to the client."

    (test/is (= "response body from file"
                (:body (t {:response {:body (io/file "test/ring/response_test.utf-8.txt")}}))))

    (test/is (= "response body from file: привет"
                (:body (t {:response {:body (io/file "test/ring/response_test.windows-1251.txt")
                                      :headers {"Content-Type" "text/plain; charset=Windows-1251"}}}))))

    )

  (testing "ByteBuffer (non-standard)"

    (test/is (= "body"
                (:body (t {:response {:body (-> "body" (.getBytes "UTF-8") (ByteBuffer/wrap))}}))))

    )

  (testing "HttpHandler (non-standard)"

    (test/is (= "body"
                (:body (t {:response {:body (reify HttpHandler (handleRequest [_ exchange]
                                                                 (doto (.getResponseSender exchange)
                                                                   (.send "body"))))}}))))

    )

  )

(deftest response-headers-t

  (testing "Set header."

    (test/is (= "test-header"
                (-> (t {:response {:headers {"X-Test-Header" "test-header"}}})
                    :headers
                    (get "x-test-header"))))

    (test/is (= nil
                (-> (t {:response {:headers {"X-Test-Header" nil}}})
                    :headers
                    (get "x-test-header"))))

    (test/is (= ["1" "2" "3"]
                (-> (t {:response {:headers {"X-Test-Header" ["1" "2" "3"]}}})
                    :headers
                    (get "x-test-header"))))

    (test/is (= ["1" "2" "3"]
                (-> (t {:response {:headers {"X-Test-Header" (seq ["1" "2" "3"])}}})
                    :headers
                    (get "x-test-header"))))

    )

  (testing "Remove header set by server middleware (non-standard)."

    (test/is (= "test-header"
                (-> (t {:wrap-handler [{:type handler/set-response-header
                                        :header {"X-Test-Header" "test-header"}}]
                        :response {}})
                    :headers
                    (get "x-test-header"))))

    (test/is (= nil
                (-> (t {:wrap-handler [{:type handler/set-response-header
                                        :header {"X-Test-Header" "test-header"}}]
                        :response {:headers {"X-Test-Header" nil}}})
                    :headers
                    (get "x-test-header"))))

    )

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
