(defproject com.github.omkamra/jnr "0.1.0-SNAPSHOT"
  :description "A Clojure wrapper for JNR-FFI"
  :url "https://github.com/omkamra/jnr"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.jnr/jnr-ffi "2.2.10"]
                 [insn/insn "0.5.2"]]
  :profiles
  {:dev {:resource-paths ["test-resources"]}})
