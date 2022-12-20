(ns strojure.undertow-ring.impl.session
  "Implementation of ring sessions."
  (:require [strojure.undertow.api.exchange :as exchange]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^:const ring-session-key
  "The name of the key in Undertow session storage for ring  session data. Same
  name as in other implementations like immutant and luminus."
  "ring-session-data")

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn get-session
  "Returns ring session data from server exchange."
  [exchange]
  (some-> (exchange/get-existing-session exchange)
          (.getAttribute ring-session-key)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn put-session-entries
  "Puts [ring session][1] `entries` into Undertow session storage.

  - When `entries` is `nil` then session data is removed.
  - When entry value is `nil` then this entry is unset from the session data.
  - Otherwise entry is set in session data.

  Raises exception if session storage is not initialized in server configuration
  and session values cannot be set.

  [1]: https://github.com/ring-clojure/ring/wiki/Sessions
  "
  [exchange, entries]
  (if-let [session (exchange/get-or-create-session exchange)]
    (.setAttribute session ring-session-key
                   (some->> entries (reduce-kv (fn [data k v]
                                                 (if (some? v) (assoc data k v)
                                                               (dissoc data k)))
                                               (.getAttribute session ring-session-key))))
    (when entries
      (throw (ex-info "Attempt to put session entries when sessions are disabled"
                      {:undertow/exchange exchange})))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
