(ns strojure.ring-undertow.server
  (:require [strojure.ring-undertow.handler :as ring.handler]
            [strojure.undertow.api.types :as types]
            [strojure.undertow.server :as server])
  (:import (clojure.lang MultiFn)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmulti handler-type-adapter
  "Returns handler function adapter for `:ring-handler-type` key in server
  configuration."
  :ring-handler-type)

(.addMethod ^MultiFn handler-type-adapter nil (constantly ring.handler/sync))
(.addMethod ^MultiFn handler-type-adapter :sync (constantly ring.handler/sync))
(.addMethod ^MultiFn handler-type-adapter :async (constantly ring.handler/async))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn start
  "Starts server using [server/start][1] with ring handler adapter.

  Additional configurations keys.

  - `:ring-handler-type` The type of all ring handlers in the configuration.
      - No value or value `:sync` means synchronous ring handler.
      - Value `:async` means asynchronous ring handler.

  To use handlers of mixed type you can either:

  - configure handler declaratively like described in [[handler/ring]];
  - apply functions [[handler/sync]] and [[handler/async]] to your handlers explicitly.

  [1]: https://cljdoc.org/d/com.github.strojure/undertow/CURRENT/api/strojure.undertow.server#start
  "
  [config]
  (binding [types/*handler-fn-adapter* (handler-type-adapter config)]
    (server/start config)))

(def ^{:arglists '([instance])} stop
  "Stops server instance, returns nil."
  server/stop)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
