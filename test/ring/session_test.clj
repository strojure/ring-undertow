(ns ring.session-test
  (:require [clojure.test :as test :refer [deftest testing]]
            [java-http-clj.core :as http]
            [strojure.ring-undertow.server :as server]
            [strojure.undertow.api.types :as types]
            [strojure.undertow.handler :as handler])
  (:import (java.net CookieManager)))

(set! *warn-on-reflection* true)

(declare thrown?)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- req
  "Executes series of HTTP request and returns last ring request."
  [& responses]
  (let [request! (atom nil)
        handler (fn [request]
                  (reset! request! request)
                  (nth responses (-> request :headers (get "x-response-num") parse-long)))
        http-opts {:client (http/build-client {:cookie-handler (CookieManager.)})}]
    (with-open [server (server/start {:handler [{:type handler/session}
                                                handler]
                                      :port 0})]
      (let [uri (str "http://localhost:"
                     (-> server types/bean* :listenerInfo first :address :port)
                     "/")]
        (doseq [i (range (count responses))]
          (http/send {:uri uri :headers {"x-response-num" (str i)}}
                     http-opts))))
    @request!))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest ring-session-t

  (testing "Set session."

    (test/is (= nil
                (:session (req {:session {:a 1}}))))

    (test/is (= {:a 1}
                (:session (req {:session {:a 1}}
                               {}))))

    )

  (testing "Delete session."

    (test/is (= nil
                (:session (req {:session {:a 1}}
                               {:session nil}
                               {}))))

    )

  (testing "Update (overwrite) session."

    (test/is (= {:b 2}
                (:session (req {:session {:a 1}}
                               {:session {:b 2}}
                               {}))))

    )

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
