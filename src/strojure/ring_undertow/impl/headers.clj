(ns strojure.ring-undertow.impl.headers
  "Implementation of persistent map proxy over Undertow request headers."
  (:require [clojure.string :as string])
  (:import (clojure.lang APersistentMap IEditableCollection IFn IKVReduce
                         IPersistentMap MapEntry MapEquivalence Util)
           (io.undertow.util HeaderMap HeaderValues)
           (java.util Map)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- ring-header-name
  [^HeaderValues x]
  (.toLowerCase (.toString (.getHeaderName x))))

(defn- ring-header-value
  [^HeaderValues x]
  (if (< 1 (.size x))
    ;; Comma separated values.
    ;; Discussion: https://groups.google.com/g/ring-clojure/c/N6vv3JkScik
    ;; RFC: https://www.rfc-editor.org/rfc/rfc9110.html#section-5.3
    (string/join "," x)
    (or (.peekFirst x) "")))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defprotocol PersistentMap
  (to-persistent-map
    ^APersistentMap [obj]
    "Converts `obj` to persistent map."))

(extend-protocol PersistentMap HeaderMap
  (to-persistent-map
    [headers]
    (persistent! (reduce (fn [m! x] (assoc! m! (ring-header-name x) (ring-header-value x)))
                         (transient {})
                         headers))))

(deftype HeaderMapProxy [^HeaderMap headers, ^:volatile-mutable persistent-copy]
  Map
  (size
    [_]
    (.size headers))
  (get
    [this k]
    (.valAt this k))
  MapEquivalence
  IFn
  (invoke
    [_ k]
    (some-> (.get headers (str k)) ring-header-value))
  (invoke
    [_ k not-found]
    (or (some-> (.get headers (str k)) ring-header-value)
        not-found))
  IPersistentMap
  (valAt
    [_ k]
    (some-> (.get headers (str k)) ring-header-value))
  (valAt
    [_ k not-found]
    (or (some-> (.get headers (str k)) ring-header-value)
        not-found))
  (entryAt
    [this k]
    (when-let [v (.valAt this k)]
      (MapEntry. k v)))
  (containsKey
    [_ k]
    (.contains headers (str k)))
  (assoc
    [this k v]
    (-> (to-persistent-map this)
        (.assoc k v)))
  (assocEx
    [this k v]
    (if (.containsKey this k)
      (throw (Util/runtimeException "Key already present"))
      (.assoc this k v)))
  (cons
    [this o]
    (-> (to-persistent-map this)
        (.cons o)))
  (without
    [this k]
    (-> (to-persistent-map this)
        (.without (.toLowerCase (str k)))))
  (empty
    [_]
    {})
  (count
    [_]
    (.size headers))
  (seq
    [this]
    (.seq (to-persistent-map this)))
  (equiv
    [this o]
    (= o (to-persistent-map this)))
  (iterator
    [this]
    (.iterator (to-persistent-map this)))
  IKVReduce
  (kvreduce
    [this f init]
    (.kvreduce ^IKVReduce (to-persistent-map this) f init))
  IEditableCollection
  (asTransient
    [this]
    (transient (to-persistent-map this)))
  PersistentMap
  (to-persistent-map
    [_]
    (or persistent-copy
        (set! persistent-copy (to-persistent-map headers))))
  Object
  (toString
    [_]
    (.toString headers)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn header-map-proxy
  "Returns persistent map proxy instance over Undertow's header map."
  [header-map]
  (HeaderMapProxy. header-map nil))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
