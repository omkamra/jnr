(ns omkamra.jnr.struct
  (:require
   [clojure.string :as str]
   [insn.core :as insn]
   [insn.util :refer [type-desc]]
   [omkamra.jnr.util :refer [qualified-class-name collect]]))

(defn array-type-desc
  [element-type]
  (type-desc (type (make-array element-type 0))))

(defn build-struct-field
  [{:keys [name type size] :as spec}]
  {:name (clojure.core/name name)
   :type (if size (array-type-desc type) type)
   :flags #{:public :final}})

(defn add-array-field
  [cls {:keys [name type size] :as spec}]
  (let [atype (array-type-desc type)]
    (collect
     [:aload 0]
     [:dup]
     [:bipush size]
     [:anewarray type]
     [:invokevirtual cls "array" [atype atype]]
     [:putfield cls (clojure.core/name name) atype])))

(defn add-struct-field
  [cls {:keys [name type] :as spec}]
  (collect
   [:aload 0]
   [:dup]
   [:new type]
   [:dup]
   [:aload 1]
   [:invokespecial type :init [jnr.ffi.Runtime :void]]
   [:invokevirtual cls "inner" [jnr.ffi.Struct jnr.ffi.Struct]]
   [:checkcast type]
   [:putfield cls (clojure.core/name name) type]))

(defn add-generic-field
  [cls {:keys [name type ctor-args] :as spec}]
  (collect
   [:aload 0]
   [:new type]
   [:dup]
   [:aload 0]
   (for [arg ctor-args]
     [:ldc arg])
   [:invokespecial type :init
    (vec (concat
          [jnr.ffi.Struct]
          (map class ctor-args)
          [:void]))]
   [:putfield cls (clojure.core/name name) type]))

(defn add-field
  [cls {:keys [type size] :as spec}]
  (cond size
        (add-array-field cls spec)
        (isa? type jnr.ffi.Struct)
        (add-struct-field cls spec)
        :else
        (add-generic-field cls spec)))

(defn build-struct
  [cls super field-specs]
  {:name cls
   :flags #{:public :super}
   :super super
   :fields
   (mapv build-struct-field field-specs)
   :methods
   [{:name :init
     :flags #{:public}
     :desc [jnr.ffi.Runtime :void]
     :emit
     (collect
      [:aload 0]
      [:aload 1]
      [:invokespecial super :init [jnr.ffi.Runtime :void]]
      (map #(add-field cls %) field-specs)
      [:return])}]})

(defn resolve-struct-field-tag
  [tag]
  (case tag
    char jnr.ffi.Struct$Signed8
    (byte unsigned-char) jnr.ffi.Struct$Unsigned8

    short jnr.ffi.Struct$Signed16
    unsigned-short jnr.ffi.Struct$Unsigned16
    int jnr.ffi.Struct$Signed32
    unsigned-int jnr.ffi.Struct$Unsigned32
    long jnr.ffi.Struct$SignedLong
    unsigned-long jnr.ffi.Struct$UnsignedLong
    long-long jnr.ffi.Struct$Signed64
    unsigned-long-long jnr.ffi.Struct$Unsigned64

    int8_t jnr.ffi.Struct$int8_t
    uint8_t jnr.ffi.Struct$u_int8_t
    int16_t jnr.ffi.Struct$int16_t
    uint16_t jnr.ffi.Struct$u_int16_t
    int32_t jnr.ffi.Struct$int32_t
    uint32_t jnr.ffi.Struct$u_int32_t
    int64_t jnr.ffi.Struct$int64_t
    uint64_t jnr.ffi.Struct$u_int64_t

    float jnr.ffi.Struct$Float
    double jnr.ffi.Struct$Double

    (* Pointer) jnr.ffi.Struct$Pointer
    (resolve tag)))

(defn resolve-struct-field-spec
  [spec]
  (if (symbol? spec)
    (recur [spec nil])
    (let [[name size] spec
          tag (:tag (meta name))
          cls (resolve-struct-field-tag tag)]
      (when-not (class? cls)
        (throw (ex-info "invalid struct field spec" {:spec spec})))
      (if (.isEnum cls)
        {:name name
         :type jnr.ffi.Struct$Enum
         :ctor-args [cls]
         :size size}
        {:name name
         :type cls
         :ctor-args []
         :size size}))))

(defmacro define-with-super
  {:style/indent 1}
  [super name & field-specs]
  (let [cls (qualified-class-name name)
        cls-parts (str/split cls #"\.")
        ns-sym (symbol (str/join "." (butlast cls-parts)))
        name-sym (symbol (last cls-parts))
        field-specs (map resolve-struct-field-spec field-specs)
        t (build-struct cls super field-specs)]
    `(do
       (insn/define ~t)
       (import '(~ns-sym ~name-sym)))))

(defmacro define
  {:style/indent 1}
  [struct-name & field-specs]
  `(define-with-super jnr.ffi.Struct ~struct-name ~@field-specs))
