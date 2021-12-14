(ns omkamra.jnr.util
  (:require
   [clojure.string :as str]))

(defn qualified-name?
  [s]
  (<= 0 (.indexOf s (int \.))))

(defn qualified-class-name
  [class-name]
  (let [s (name class-name)]
    (if (qualified-name? s)
      s
      (str (munge (ns-name *ns*)) "." s))))

(defn alloc-temp-int
  [$lib]
  (let [runtime (jnr.ffi.Runtime/getRuntime $lib)
        int-type jnr.ffi.NativeType/SINT]
    (jnr.ffi.Memory/allocateTemporary runtime int-type)))

(defn alloc-temp-double
  [$lib]
  (let [runtime (jnr.ffi.Runtime/getRuntime $lib)
        double-type jnr.ffi.NativeType/DOUBLE]
    (jnr.ffi.Memory/allocateTemporary runtime double-type)))

(defn alloc-temp-buffer
  [$lib size]
  (let [runtime (jnr.ffi.Runtime/getRuntime $lib)
        mm (.getMemoryManager runtime)]
    (.allocateTemporary mm size false)))

(defn instruction?
  [item]
  (and (vector? item) (keyword? (first item))))

(defn collect
  [& items]
  (loop [items items
         result []]
    (if-let [item (first items)]
      (cond (instruction? item)
            (recur (next items) (conj result item))
            (sequential? item)
            (recur (concat item (next items)) result)
            :else (throw (ex-info "cannot collect item" {:item item})))
      result)))
