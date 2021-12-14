(ns omkamra.jnr.union
  (:require [omkamra.jnr.struct :refer [define-with-super]]))

(defmacro define
  {:style/indent 1}
  [union-name & field-specs]
  `(define-with-super jnr.ffi.Union ~union-name ~@field-specs))
