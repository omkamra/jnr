(ns omkamra.jnr.union
  (:require [omkamra.jnr.struct :refer [define-struct-or-union]]))

(defmacro define
  {:style/indent 1}
  [union-name & field-specs]
  `(define-struct-or-union :union ~union-name ~@field-specs))
