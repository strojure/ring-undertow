(ns ring.request-test
  (:require [clojure.test :as test :refer [deftest testing]]
            [java-http-clj.core :as http]
            [strojure.ring-undertow.server :as server]
            [strojure.undertow.api.types :as types]))

(set! *warn-on-reflection* true)

(declare thrown?)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- req
  "Executes HTTP request and returns corresponding ring request."
  [{:keys [method, uri, headers, body] :or {method :get, uri "/", headers {}}}]
  (let [request-promise (promise)
        handler (fn [{:keys [body] :as request}]
                  (deliver request-promise (cond-> request
                                             body (assoc ::body-content (slurp body))))
                  {:status 200})]
    (with-open [server (server/start {:handler handler :port 0})]
      (http/send {:method method
                  :uri (str "http://localhost:"
                            (-> server types/bean* :listenerInfo first :address :port)
                            uri)
                  :headers headers
                  :body body}))
    @request-promise))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest request-map-keys-t

  (testing "Request without :body"

    (test/is (= #{:headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (req {:method :get})
                     (keys)
                     (filter #{:headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    (test/is (= #{:headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (req {:method :post})
                     (keys)
                     (filter #{:headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    )

  (testing "Request with :body"

    (test/is (= #{:body :headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (req {:method :get, :body "body"})
                     (keys)
                     (filter #{:body :headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    (test/is (= #{:body :headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (req {:method :post, :body "body"})
                     (keys)
                     (filter #{:body :headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    )

  (testing "Request with :query-string"

    (test/is (= #{:query-string :headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (req {:uri "/?param=value"})
                     (keys)
                     (filter #{:query-string :headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    )

  )

(deftest request-scheme-t

  (test/is (= :http
              (:scheme (req {:method :get}))))

  )

(deftest request-method-t

  (test/is (= :get
              (:request-method (req {:method :get}))))

  (test/is (= :post
              (:request-method (req {:method :post}))))

  )

(deftest request-uri-t

  (test/is (= "/"
              (:uri (req {:uri "/"}))))

  (test/is (= "/path"
              (:uri (req {:uri "/path"}))))

  (test/is (= "/path"
              (:uri (req {:uri "/path?param=value"}))))

  )

(deftest request-query-string-t

  (test/is (= nil
              (:query-string (req {:uri "/"}))))

  (test/is (= nil
              (:query-string (req {:uri "/path"}))))

  (test/is (= nil
              (:query-string (req {:uri "/path?"}))))

  (test/is (= "param=value"
              (:query-string (req {:uri "/path?param=value"}))))

  )

(deftest request-headers-t

  (test/is (= nil
              (-> (:headers (req {:headers {}}))
                  (get "x-test-header"))))

  (test/is (= "test"
              (-> (:headers (req {:headers {"X-Test-Header" "test"}}))
                  (get "x-test-header"))))

  (testing "case insensitivity (non-standard)"

    (test/is (= "test"
                (-> (:headers (req {:headers {"X-Test-Header" "test"}}))
                    (get "X-Test-Header"))))

    )

  )

(deftest request-body-t

  (test/is (= nil
              (::body-content (req {:method :get}))))

  (test/is (= "body"
              (::body-content (req {:method :get, :body "body"}))))

  (test/is (= nil
              (::body-content (req {:method :post}))))

  (test/is (= "body"
              (::body-content (req {:method :post, :body "body"}))))

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
