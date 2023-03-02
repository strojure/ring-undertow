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

(defn- exec
  "Executes seq of ring handlers and returns seq of `{:request _ :response _}`
  on server side."
  [{:keys [handlers]}]
  (let [result! (atom [])
        handler (fn [request]
                  (let [handler (nth handlers (-> request :headers (get "x-response-num") parse-long))
                        response (handler request)]
                    (swap! result! conj {:request request :response response})
                    response))
        http-opts {:client (http/build-client {:cookie-handler (CookieManager.)})}]
    (with-open [server (server/start {:handler [{:type handler/session}
                                                handler]
                                      :port 0})]
      (let [uri (str "http://localhost:"
                     (-> server types/bean* :listenerInfo first :address :port)
                     "/")]
        (doseq [i (range (count handlers))]
          (http/send {:uri uri :headers {"x-response-num" (str i)}}
                     http-opts))))
    @result!))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest ring-session-t

  (testing "Set session."

    (test/is (= nil
                (:session (:request (last (exec {:handlers [(constantly {:session {:a 1}})]}))))))

    (test/is (= {:a 1}
                (:session (:request (last (exec {:handlers [(constantly {:session {:a 1}})
                                                            (constantly {})]}))))))

    )

  (testing "Delete session."

    (test/is (= nil
                (:session (:request (last (exec {:handlers [(constantly {:session {:a 1}})
                                                            (constantly {:session nil})
                                                            (constantly {})]}))))))

    (testing "Delete session cookie when session deleted."
      (test/is (= nil
                  (-> (last (exec {:handlers [(constantly {:session {:a 1}})
                                              (constantly {:session nil})
                                              (constantly {})]}))
                      :request :headers
                      (get "cookie")))))

    )

  (testing "Update (overwrite) session."

    (test/is (= {:b 2}
                (:session (:request (last (exec {:handlers [(constantly {:session {:a 1}})
                                                            (constantly {:session {:b 2}})
                                                            (constantly {})]}))))))

    (test/is (= {:a 2}
                (:session (:request (last (exec {:handlers [(constantly {:session {:a 1}})
                                                            (fn [{:keys [session]}]
                                                              {:session (update session :a inc)})
                                                            (constantly {})]}))))))

    )

  (testing "Recreate (rename) session."

    (testing "Session ID does not change."
      (test/is (= 1 #_"number of unique non-nil cookie headers"
                  (->> (exec {:handlers [(constantly {:session {:a 1}})
                                         (constantly {:session {:a 1}})
                                         (constantly {})]})
                       (keep (comp #(get % "cookie") :headers :request))
                       (distinct)
                       (count)))))

    (testing "Session ID changes."
      (test/is (= 2 #_"number of unique non-nil cookie headers"
                  (->> (exec {:handlers [(constantly {:session {:a 1}})
                                         (constantly {:session ^:recreate {:a 1}})
                                         (constantly {})]})
                       (keep (comp #(get % "cookie") :headers :request))
                       (distinct)
                       (count)))))

    )

  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
