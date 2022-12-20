(ns strojure.undertow-ring.impl.request-headers
  (:require [clojure.string :as string])
  (:import (clojure.lang APersistentMap IEditableCollection IFn IKVReduce
                         IPersistentMap MapEntry MapEquivalence RT Util)
           (io.undertow.util HeaderMap HeaderValues)
           (java.util Map)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn header-name
  [^HeaderValues x]
  (.toLowerCase (.toString (.getHeaderName x))))

(defn header-value
  [^HeaderValues x]
  (if (< 1 (.size x))
    ;; Comma separated values.
    ;; Discussion: https://groups.google.com/g/ring-clojure/c/N6vv3JkScik
    ;; RFC: https://www.rfc-editor.org/rfc/rfc9110.html#section-5.3
    (string/join "," x)
    (or (.peekFirst x) "")))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defprotocol PersistentMap
  (as-persistent-map ^APersistentMap [_]))

(extend-protocol PersistentMap HeaderMap
  (as-persistent-map
    [headers]
    (persistent! (reduce (fn [m! x] (assoc! m! (header-name x) (header-value x)))
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
    (some-> (.get headers (str k)) header-value))
  (invoke
    [_ k not-found]
    (or (some-> (.get headers (str k)) header-value)
        not-found))
  IPersistentMap
  (valAt
    [_ k]
    (some-> (.get headers (str k)) header-value))
  (valAt
    [_ k not-found]
    (or (some-> (.get headers (str k)) header-value)
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
    (-> (as-persistent-map this)
        (assoc k v)))
  (assocEx
    [this k v]
    (if (.containsKey this k)
      (throw (Util/runtimeException "Key already present"))
      (assoc this k v)))
  (cons
    [this o]
    (-> (as-persistent-map this)
        (conj o)))
  (without
    [this k]
    (-> (as-persistent-map this)
        (dissoc (.toLowerCase (str k)))))
  (empty
    [_]
    {})
  (count
    [_]
    (.size headers))
  (seq
    [this]
    (seq (as-persistent-map this)))
  (equiv
    [this o]
    (= o (as-persistent-map this)))
  (iterator
    [this]
    (.iterator (as-persistent-map this)))
  IKVReduce
  (kvreduce
    [this f init]
    (.kvreduce ^IKVReduce (as-persistent-map this) f init))
  IEditableCollection
  (asTransient
    [this]
    (transient (as-persistent-map this)))
  PersistentMap
  (as-persistent-map
    [_]
    (or persistent-copy
        (set! persistent-copy (as-persistent-map headers))))
  Object
  (toString
    [this]
    (RT/printString this)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn ring-headers
  [header-map]
  (HeaderMapProxy. header-map nil))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
