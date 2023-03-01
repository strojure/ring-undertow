(ns strojure.ring-undertow.server
  "Server's start/stop functionality with ring handler adapter."
  (:require [strojure.ring-undertow.handler :as ring.handler]
            [strojure.undertow.api.types :as types]
            [strojure.undertow.server :as server])
  (:import (strojure.undertow.api.types ServerInstance)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn start
  "Starts server using [server/start][1] with ring handler adapter.

  Additional configurations keys.

  - `:ring-handler-type` The type of all ring handlers in the configuration.
      - No value or value `:sync` means synchronous ring handler
        [[handler/sync]].
      - Value `:async` means asynchronous ring handler [[handler/async]].

  To use handlers of mixed type you can either:

  - configure handler declaratively like described in [[handler/ring]];
  - apply functions [[handler/sync]] and [[handler/async]] to your handlers
    explicitly.

  [1]: https://cljdoc.org/d/com.github.strojure/undertow/CURRENT/api/strojure.undertow.server#start
  "
  {:tag ServerInstance}
  [config]
  (binding [types/*handler-fn-adapter* (ring.handler/adapter config)]
    (server/start config)))

(def ^{:arglists '([instance])} stop
  "Stops server instance, returns nil."
  server/stop)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
