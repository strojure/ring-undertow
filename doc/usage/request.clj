(ns usage.request
  (:require [strojure.ring-undertow.request :as request]))

;; Check if sessions are enabled in Undertow server configuration.

(request/sessions-enabled? {})
;=> nil

;; Check if request is websocket upgrade request.

(request/websocket? {})
;=> nil

(request/websocket? {:headers {"upgrade" "unknown"}})
;=> false

(request/websocket? {:headers {"upgrade" "websocket"}})
;=> true
