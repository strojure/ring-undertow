(ns strojure.undertow-ring.impl.session
  (:require [strojure.undertow.api.exchange :as exchange])
  (:import (io.undertow.server HttpServerExchange)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^:const ring-session-key "ring-session-data")

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn get-session
  [exchange]
  (some-> (exchange/get-existing-session exchange)
          (.getAttribute ring-session-key)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn update-values
  [^HttpServerExchange e, values]
  (if-let [session! (exchange/get-or-create-session e)]
    (.setAttribute session! ring-session-key
                   (some->> values (reduce-kv (fn [session k v]
                                                (if (some? v) (assoc session k v)
                                                              (dissoc session k)))
                                              (.getAttribute session! ring-session-key))))
    (when values
      (throw (ex-info "Attempt to set session values when sessions are disabled"
                      {:undertow/exchange e})))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
