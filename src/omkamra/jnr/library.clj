(ns omkamra.jnr.library
  (:refer-clojure :exclude [load])
  (:require [clojure.string :as str])
  (:import (java.io File)
           (jnr.ffi.annotations LongLong)
           (jnr.ffi.types int8_t int16_t int32_t int64_t
                          u_int8_t u_int16_t u_int32_t u_int64_t
                          off_t size_t ssize_t)))

(defn resolve-annotation
  [sym]
  (try
    (let [annotation (Class/forName (str "jnr.ffi.types." sym))]
      {:tag 'long annotation true})
    (catch ClassNotFoundException e)))

(defn resolve-class-name
  [sym]
  (when-let [result (resolve sym)]
    (when (class? result)
      {:tag (symbol (.getName result))})))

(defn tag->meta
  [tag]
  (when (symbol? tag)
    (case tag
      (char unsigned-char) {:tag 'byte}
      (short unsigned-short) {:tag 'short}
      (int unsigned-int) {:tag 'int}
      (long unsigned-long) {:tag 'long}
      (long-long unsigned-long-long) {:tag 'long LongLong true}
      Pointer {:tag 'jnr.ffi.Pointer}
      int8_t {:tag 'byte int8_t true}
      int16_t {:tag 'short int16_t true}
      int32_t {:tag 'int int32_t true}
      int64_t {:tag 'long int64_t true}
      (uint8_t u_int8_t) {:tag 'byte u_int8_t true}
      (uint16_t u_int16_t) {:tag 'short u_int16_t true}
      (uint32_t u_int32_t) {:tag 'int u_int32_t true}
      (uint64_t u_int64_t) {:tag 'long u_int64_t true}
      (or (resolve-annotation tag)
          (resolve-class-name tag)))))

(defn resolve-meta
  [x]
  (if-let [tag (:tag (meta x))]
    (vary-meta x merge (tag->meta tag))
    x))

(defmacro define-interface
  [iface-name & sigs]
  `(clojure.core/definterface ~iface-name
     ~@(map (fn [[method-name method-args]]
              (list (resolve-meta method-name)
                    (mapv resolve-meta method-args)))
         sigs)))

(defn loader
  [iface]
  (jnr.ffi.LibraryLoader/create iface))

(defn load
  [iface lib-name]
  (-> (loader iface)
      (.load lib-name)))

(defmacro define
  {:style/indent 2}
  [name lib-name & sigs]
  (let [iface-name (symbol (str "omkamra_jnr_interface_" name))]
    `(do
       (define-interface ~iface-name ~@sigs)
       (def ~name (load ~iface-name ~lib-name)))))

(defn runtime
  [$lib]
  (jnr.ffi.Runtime/getRuntime $lib))

(defn errno
  ([$lib]
   (.getLastError (runtime $lib)))
  ([$lib value]
   (.setLastError (runtime $lib) value)))
