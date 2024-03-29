(ns omkamra.jnr.struct
  (:require
   [clojure.string :as str]
   [insn.core :as insn]
   [insn.util :refer [type-desc]]
   [omkamra.jnr.util :refer [qualified-class-name collect]]))

(defn array-type-desc
  [element-type]
  (type-desc (type (make-array element-type 0))))

(defn struct-field-type
  ([type size]
   (if size
     (array-type-desc (struct-field-type type))
     (if (.isEnum type)
       jnr.ffi.Struct$Enum
       type)))
  ([type]
   (struct-field-type type nil)))

(defn build-struct-field
  [{:keys [name type size] :as spec}]
  {:name (clojure.core/name name)
   :type (struct-field-type type size)
   :flags #{:public :final}})

(defn add-array-field
  [cls {:keys [name type size] :as spec}]
  (let [element-type (struct-field-type type)
        atd (array-type-desc element-type)]
    (collect
     [:aload 0]
     [:dup]
     [:ldc size]
     [:anewarray element-type]
     (cond (isa? element-type jnr.ffi.Struct)
           (let [atd (array-type-desc jnr.ffi.Struct)]
             [:invokevirtual cls "array" [atd atd]])
           (isa? element-type jnr.ffi.Struct$Enum)
           (let [atd (array-type-desc jnr.ffi.Struct$Enum)]
             (vector
              [:ldc type]
              [:invokevirtual cls "array" [atd (class type) atd]]))
           :else
           [:invokevirtual cls "array" [atd atd]])
     [:checkcast atd]
     [:putfield cls (clojure.core/name name) atd])))

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

(defn add-enum-field
  [cls {:keys [name type] :as spec}]
  (collect
   [:aload 0]
   [:new jnr.ffi.Struct$Enum]
   [:dup]
   [:aload 0]
   [:ldc type]
   [:invokespecial jnr.ffi.Struct$Enum :init [jnr.ffi.Struct (class type) :void]]
   [:putfield cls (clojure.core/name name) jnr.ffi.Struct$Enum]))

(defn add-generic-field
  [cls {:keys [name type] :as spec}]
  (collect
   [:aload 0]
   [:new type]
   [:dup]
   [:aload 0]
   [:invokespecial type :init [jnr.ffi.Struct :void]]
   [:putfield cls (clojure.core/name name) type]))

(defn add-field
  [cls {:keys [type size] :as spec}]
  (cond size
        (add-array-field cls spec)
        (isa? type jnr.ffi.Struct)
        (add-struct-field cls spec)
        (.isEnum type)
        (add-enum-field cls spec)
        :else
        (add-generic-field cls spec)))

(defn build-struct
  [cls super field-specs alignment]
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
      (if alignment
        (vector
         [:new jnr.ffi.Struct$Alignment]
         [:dup]
         [:ldc alignment]
         [:invokespecial jnr.ffi.Struct$Alignment :init [:int :void]]
         [:invokespecial super :init [jnr.ffi.Runtime jnr.ffi.Struct$Alignment :void]])
        (vector
         [:invokespecial super :init [jnr.ffi.Runtime :void]]))
      (map #(add-field cls %) field-specs)
      [:return])}]})

(defn resolve-integer-alias
  [sym]
  (try
    (Class/forName (str "jnr.ffi.Struct$" sym))
    (catch ClassNotFoundException e)))

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

    (or (resolve-integer-alias tag)
        (resolve tag))))

(defn valid-ffi-struct-member?
  [cls]
  (and (class? cls)
       (or (isa? cls jnr.ffi.Struct$Member)
           (isa? cls jnr.ffi.Struct)
           (.isEnum cls))))

(defn resolve-struct-field-spec
  [spec]
  (if (symbol? spec)
    (recur [spec nil])
    (let [[name size] spec
          tag (:tag (meta name))
          cls (resolve-struct-field-tag tag)]
      (when-not (valid-ffi-struct-member? cls)
        (throw (ex-info "invalid struct field spec" {:spec spec})))
      {:name name
       :type cls
       :size size})))

(defmacro define-struct-or-union
  {:style/indent 1}
  [struct-or-union name & field-specs]
  (let [cls (qualified-class-name name)
        cls-parts (str/split cls #"\.")
        ns-sym (symbol (str/join "." (butlast cls-parts)))
        name-sym (symbol (last cls-parts))
        field-specs (map resolve-struct-field-spec field-specs)
        struct? (= :struct struct-or-union)
        super (if struct? 'jnr.ffi.Struct 'jnr.ffi.Union)
        metadata (meta name)
        alignment (when struct? (if (:packed metadata) 1 0))
        t (build-struct cls super field-specs alignment)]
    `(do
       (insn/define ~t)
       (import '(~ns-sym ~name-sym)))))

(defmacro define
  {:style/indent 1}
  [struct-name & field-specs]
  `(define-struct-or-union :struct ~struct-name ~@field-specs))
