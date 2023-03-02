(ns strojure.ring-undertow.impl.session
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

(defn put-session
  "Puts [ring session][1] `data` into Undertow session storage or remove it if
  `data` is null. Changes session identifier if `(-> data meta :recreate)` is
  `true`.

  Raises exception if session storage is not initialized in server configuration
  and session values cannot be set.

  [1]: https://github.com/ring-clojure/ring/wiki/Sessions
  "
  [exchange, data]
  (if (some? data)
    ;; Put non-nil data into session.
    (if-let [session (exchange/get-or-create-session exchange)]
      (do (.setAttribute session ring-session-key data)
          ;; Change session identifier if :recreate meta is true.
          (when (some-> (meta data) :recreate)
            (.changeSessionId session exchange (exchange/get-session-config exchange))))
      (throw (ex-info "Attempt to put session entries when sessions are disabled"
                      {:server-exchange exchange})))
    ;; Destroy session if `data` is nil.
    (some-> (exchange/get-existing-session exchange)
            (.invalidate exchange))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
