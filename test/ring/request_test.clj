(ns ring.request-test
  (:require [clojure.test :as test :refer [deftest testing]]
            [java-http-clj.core :as http]
            [strojure.ring-undertow.server :as server]
            [strojure.undertow.api.types :as types]))

(set! *warn-on-reflection* true)

(declare thrown?)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- exec
  "Executes HTTP request and returns map `{:request request}` with corresponding
  ring request on server side."
  [{:keys [method, uri, headers, body, request-fn] :or {method :get, uri "/", headers {}}}]
  (let [request-promise (promise)
        handler (fn [request]
                  (deliver request-promise (cond-> request request-fn (request-fn)))
                  {:status 200})]
    (with-open [server (server/start {:handler handler :port 0})]
      (http/send {:method method
                  :uri (str "http://localhost:"
                            (-> server types/bean* :listenerInfo first :address :port)
                            uri)
                  :headers headers
                  :body body}))
    {:request @request-promise}))

(defn- read-body-content
  "A request-fn for exec to read :body stream and assoc it as :body-content into
  request."
  [{:keys [body] :as request}]
  (assoc request :body-content (some-> body (slurp))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest request-map-keys-t

  (testing "Request without :body"

    (test/is (= #{:headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (exec {:method :get})
                     :request (keys)
                     (filter #{:headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    (test/is (= #{:headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (exec {:method :post})
                     :request (keys)
                     (filter #{:headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    )

  (testing "Request with :body"

    (test/is (= #{:body :headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (exec {:method :get, :body "body"})
                     :request (keys)
                     (filter #{:body :headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    (test/is (= #{:body :headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (exec {:method :post, :body "body"})
                     :request (keys)
                     (filter #{:body :headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    )

  (testing "Request with :query-string"

    (test/is (= #{:query-string :headers :remote-addr :request-method :scheme :server-name :server-port :uri}
                (->> (exec {:uri "/?param=value"})
                     :request (keys)
                     (filter #{:query-string :headers :remote-addr :request-method :scheme :server-name :server-port :uri})
                     (set))))

    )

  )

(deftest request-scheme-t

  (test/is (= :http
              (:scheme (:request (exec {:method :get})))))

  )

(deftest request-method-t

  (test/is (= :get
              (:request-method (:request (exec {:method :get})))))

  (test/is (= :post
              (:request-method (:request (exec {:method :post})))))

  )

(deftest request-uri-t

  (test/is (= "/"
              (:uri (:request (exec {:uri "/"})))))

  (test/is (= "/path"
              (:uri (:request (exec {:uri "/path"})))))

  (test/is (= "/path"
              (:uri (:request (exec {:uri "/path?param=value"})))))

  )

(deftest request-query-string-t

  (test/is (= nil
              (:query-string (:request (exec {:uri "/"})))))

  (test/is (= nil
              (:query-string (:request (exec {:uri "/path"})))))

  (test/is (= nil
              (:query-string (:request (exec {:uri "/path?"})))))

  (test/is (= "param=value"
              (:query-string (:request (exec {:uri "/path?param=value"})))))

  )

(deftest request-headers-t

  (test/is (= nil
              (-> (:headers (:request (exec {:headers {}})))
                  (get "x-test-header"))))

  (test/is (= "test"
              (-> (:headers (:request (exec {:headers {"X-Test-Header" "test"}})))
                  (get "x-test-header"))))

  (testing "case insensitivity (non-standard)"

    (test/is (= "test"
                (-> (:headers (:request (exec {:headers {"X-Test-Header" "test"}})))
                    (get "X-Test-Header"))))

    )

  )

(deftest request-body-t

  (test/is (= nil
              (:body-content (:request (exec {:method :get
                                              :request-fn read-body-content})))))

  (test/is (= "body"
              (:body-content (:request (exec {:method :get, :body "body"
                                              :request-fn read-body-content})))))

  (test/is (= nil
              (:body-content (:request (exec {:method :post
                                              :request-fn read-body-content})))))

  (test/is (= "body"
              (:body-content (:request (exec {:method :post, :body "body"
                                              :request-fn read-body-content})))))

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
