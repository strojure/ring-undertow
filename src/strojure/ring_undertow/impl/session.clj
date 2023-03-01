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

;; TODO: Handle session `:recreate`

(defn put-session
  "Puts [ring session][1] `data` into Undertow session storage or remove it if
  `data` is null.

  Raises exception if session storage is not initialized in server configuration
  and session values cannot be set.

  [1]: https://github.com/ring-clojure/ring/wiki/Sessions
  "
  [exchange, data]
  (if-let [session (exchange/get-or-create-session exchange)]
    (.setAttribute session ring-session-key data)
    (when data
      (throw (ex-info "Attempt to put session entries when sessions are disabled"
                      {:server-exchange exchange})))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
