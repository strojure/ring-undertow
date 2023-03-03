(ns strojure.ring-undertow.request-test
  (:require [clojure.test :as test :refer [deftest]]
            [java-http-clj.core :as http]
            [java-http-clj.websocket :as websocket]
            [strojure.ring-undertow.request :as request]
            [strojure.ring-undertow.server :as server]
            [strojure.undertow.api.types :as types]
            [strojure.undertow.handler :as handler]))

(set! *warn-on-reflection* true)

(declare thrown?)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- exec
  "Executes HTTP request and returns map `{:request request}` with corresponding
  ring request on server side."
  [{:keys [wrappers, method, uri, headers, body, request-fn] :or {method :get, uri "/", headers {}}}]
  (let [request-promise (promise)
        handler (fn [request]
                  (deliver request-promise (cond-> request request-fn (request-fn)))
                  {:status 200})]
    (with-open [server (server/start {:handler (cond->> handler wrappers (conj wrappers))
                                      :port 0})]
      (http/send {:method method
                  :uri (str "http://localhost:"
                            (-> server types/bean* :listenerInfo first :address :port)
                            uri)
                  :headers headers
                  :body body}))
    {:request @request-promise}))

(defn- exec-ws
  "Executes websocket connection request and returns map `{:request request}`
  with corresponding ring request on server side."
  [{:keys [wrappers, uri, request-fn] :or {uri "/"}}]
  (let [request-promise (promise)
        handler (fn [request]
                  (deliver request-promise (cond-> request request-fn (request-fn)))
                  {:body (handler/websocket {})})]
    (with-open [server (server/start {:handler (cond->> handler wrappers (conj wrappers))
                                      :port 0})]
      (-> (websocket/build-websocket (str "ws://localhost:"
                                          (-> server types/bean* :listenerInfo first :address :port)
                                          uri)
                                     {})
          (websocket/close)))
    {:request @request-promise}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest sessions-enabled?-t

  (test/is (= false
              (request/sessions-enabled? (:request (exec {})))))

  (test/is (= true
              (request/sessions-enabled? (:request (exec {:wrappers [{:type handler/session}]})))))

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest websocket?-t

  (test/is (= false
              (request/websocket? (:request (exec {})))))

  (test/is (= true
              (request/websocket? (:request (exec-ws {})))))

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
