(ns strojure.ring-undertow.handler-test
  (:require [clojure.test :as test :refer [deftest testing]]
            [java-http-clj.core :as http]
            [strojure.ring-undertow.handler :as handler]
            [strojure.ring-undertow.server :as server]
            [strojure.undertow.api.types :as types]))

(set! *warn-on-reflection* true)

(declare thrown?)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- exec
  "Executes HTTP request and returns map `{:response response}` with HTTP
  response."
  [{:keys [handler, method, uri, headers, body]
    :or {handler (constantly {}) method :get, uri "/", headers {}}}]
  (with-open [server (server/start {:handler handler :port 0})]
    {:response (http/send {:method method
                           :uri (str "http://localhost:"
                                     (-> server types/bean* :listenerInfo first :address :port)
                                     uri)
                           :headers headers
                           :body body})}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest async-t

  (testing "Async timeout"

    (test/is (= 300
                (:status (:response (exec {:handler (-> (fn [_ respond _]
                                                          (future
                                                            (Thread/sleep 0)
                                                            (respond {:status 300})))
                                                        (handler/async 1000))})))))

    (test/is (= 500
                (:status (:response (exec {:handler (-> (fn [_ respond _]
                                                          (future
                                                            (Thread/sleep 2000)
                                                            (respond {:status 300})))
                                                        (handler/async 1000))})))))

    )

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

