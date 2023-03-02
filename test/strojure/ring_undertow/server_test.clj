(ns strojure.ring-undertow.server-test
  (:require [clojure.test :as test :refer [deftest testing]]
            [java-http-clj.core :as http]
            [strojure.ring-undertow.server :as server]
            [strojure.undertow.api.types :as types]))

(set! *warn-on-reflection* true)

(declare thrown?)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- exec
  "Executes HTTP request and returns map `{:response response}` with HTTP
  response."
  [{:keys [config]
    {:keys [method, uri, headers, body] :or {method :get, uri "/", headers {}}} :request}]
  (with-open [server (server/start (merge {:port 0} config))]
    {:response (http/send {:method method
                           :uri (str "http://localhost:"
                                     (-> server types/bean* :listenerInfo first :address :port)
                                     uri)
                           :headers headers
                           :body body})}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest start-t

  (testing "Sync handler"

    (test/is (= 300
                (:status (:response (exec {:config {:handler (fn [_] {:status 300})}})))))

    )

  (testing "Async handler"

    (test/is (= 300
                (:status (:response (exec {:config {:ring-handler-type :async
                                                    :handler (fn [_ respond _] (respond {:status 300}))}})))))

    (test/is (= 300
                (:status (:response (exec {:config {:ring-handler-type :async
                                                    :handler (fn [_ respond _] (future (respond {:status 300})))}})))))

    )

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
