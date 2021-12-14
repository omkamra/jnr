(ns omkamra.jnr.enum
  (:require
   [clojure.string :as str]
   [insn.core :as insn]
   [insn.util :refer [type-desc]]
   [omkamra.jnr.util :refer [qualified-class-name collect]]))

(defn build-enum-field
  [name type]
  {:name name
   :type type
   :flags #{:public :static :final :enum}})

(defn build-enum
  [cls field-specs]
  {:name cls
   :flags #{:public :final :super :enum}
   :super Enum
   :fields
   (-> (mapv #(build-enum-field % cls) (map :name field-specs))
       (conj {:name "value"
              :type :int
              :flags #{:private :final}})
       (conj {:name "$VALUES"
              :type [cls]
              :flags #{:private :static :final :synthetic}}))
   :methods
   [{:name :init
     :flags #{:private}
     :desc [String :int :int :void]
     :emit [[:aload 0]
            [:aload 1]
            [:iload 2]
            [:invokespecial Enum :init [String :int :void]]
            [:aload 0]
            [:iload 3]
            [:putfield cls "value" :int]
            [:return]]}
    {:name "values"
     :flags #{:public :static}
     :desc [[cls]]
     :emit [[:getstatic cls "$VALUES" [cls]]
            [:invokevirtual (type-desc [cls]) "clone" [Object]]
            [:checkcast [cls]]
            [:areturn]]}
    {:name "valueOf"
     :flags #{:public :static}
     :desc [String cls]
     :emit [[:ldc :this]
            [:aload 0]
            [:invokestatic Enum "valueOf"]
            [:checkcast cls]
            [:areturn]]}
    {:name "intValue"
     :flags #{:public}
     :desc [:int]
     :emit [[:aload 0]
            [:getfield cls "value" :int]
            [:ireturn]]}
    {:name :clinit
     :emit
     (collect
      (map (fn [{:keys [name ordinal value] :as spec}]
             (vector
              [:new cls]
              [:dup]
              [:ldc name]
              [:ldc ordinal]
              [:ldc value]
              [:invokespecial cls :init [String :int :int :void]]
              [:putstatic cls name cls]))
           field-specs)
      [:ldc (count field-specs)]
      [:anewarray cls]
      (map (fn [{:keys [name ordinal] :as spec}]
             (vector
              [:dup]
              [:ldc ordinal]
              [:getstatic cls name cls]
              [:aastore]))
           field-specs)
      [:putstatic cls "$VALUES" [cls]]
      [:return])}]})

(defn sanitize-field-specs
  [specs]
  (loop [ordinal 0
         value 0
         specs specs
         result []]
    (if-let [spec (first specs)]
      (if (symbol? spec)
        (recur (inc ordinal)
               (inc value)
               (next specs)
               (conj result {:name (clojure.core/name spec)
                             :ordinal ordinal
                             :value value}))
        (do
          (assert (and (vector? spec) (= (count spec) 2))
                  "enum field must be a symbol or a two-element vector")
          (let [[name value] spec]
            (assert (integer? value) "enum value must be an integer")
            (recur (inc ordinal)
                   (inc value)
                   (next specs)
                   (conj result {:name (clojure.core/name name)
                                 :ordinal ordinal
                                 :value value})))))
      result)))

(defmacro define
  {:style/indent 1}
  [enum-name & field-specs]
  (let [cls (qualified-class-name enum-name)
        cls-parts (str/split cls #"\.")
        ns-sym (symbol (str/join "." (butlast cls-parts)))
        name-sym (symbol (last cls-parts))
        t (build-enum cls (sanitize-field-specs field-specs))]
    `(do
       (insn/define ~t)
       (import '(~ns-sym ~name-sym)))))
