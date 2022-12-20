(ns strojure.ring-undertow.request
  "Ring request helper functions."
  (:require [strojure.undertow.api.exchange :as exchange]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn sessions-enabled?
  "True if sessions are enabled in Undertow server configuration. When sessions
  are disabled then attempts to set :session keys will raise exception."
  [req]
  (some-> req :server-exchange exchange/sessions-enabled?))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn websocket?
  "True if `Upgrade` header in the request is \"websocket\". Case-insensitive."
  [req]
  (some-> req :headers ^String (get "upgrade")
          (.equalsIgnoreCase "websocket")))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
