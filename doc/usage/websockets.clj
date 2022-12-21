(ns usage.websockets
  (:require
    [strojure.ring-undertow.request :as request]
    [strojure.undertow.handler :as handler]
    [strojure.undertow.websocket.channel :as channel]))

(defn on-message
  "Websocket callback."
  [{:keys [channel text]}]
  (channel/send-text (str "Received: " text) channel nil))

(defn ring-handler
  "Ring handler initiating websocket connection."
  [request]
  (if (request/websocket? request)
    {:body (handler/websocket {:on-message on-message})}
    {:status 404}))
