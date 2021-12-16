(ns omkamra.jnr-test
  (:use [clojure.test])
  (:require [clojure.java.io :as jio]
            [clojure.java.shell :refer [sh]])
  (:import (java.io File)
           (java.nio ByteBuffer)
           (jnr.ffi Pointer)
           (jnr.ffi.annotations LongLong Encoding))
  (:require [omkamra.jnr.library :as library]
            [omkamra.jnr.enum :as enum]
            [omkamra.jnr.struct :as struct]
            [omkamra.jnr.union :as union]))

(enum/define omkamra_jnr_test_enum
  OMKAMRA_JNR_TEST_OK
  OMKAMRA_JNR_TEST_FAIL
  [OMKAMRA_JNR_TEST_WHO 12]
  OMKAMRA_JNR_TEST_KNOWS
  [OMKAMRA_JNR_TEST_SURREY 23]
  OMKAMRA_JNR_TEST_LAST)

(struct/define omkamra_jnr_test_point
  [^int x]
  [^int y])

(union/define omkamra_jnr_test_union
  [^char c]
  [^short s]
  [^int i]
  [^long l]
  [^float f]
  [^double d]
  [^omkamra_jnr_test_point p])

(struct/define omkamra_jnr_test_struct
  [^char c]
  [^unsigned-char uc]
  [^short s]
  [^unsigned-short us]
  [^int i]
  [^unsigned-int ui]
  [^long l]
  [^unsigned-long ul]
  [^long-long ll]
  [^unsigned-long-long ull]
  [^float f]
  [^double d]
  [^omkamra_jnr_test_enum e]
  [^omkamra_jnr_test_point p]
  [^int a 10]
  [^omkamra_jnr_test_union u])

(struct/define omkamra_jnr_test_struct_with_special_types
  ^int8_t i8
  ^uint8_t u8
  ^int16_t i16
  ^uint16_t u16
  ^int32_t i32
  ^uint32_t u32
  ^int64_t i64
  ^uint64_t u64
  ^intptr_t intptr
  ^uintptr_t uintptr
  ^caddr_t caddr
  ^dev_t dev
  ^blkcnt_t blkcnt
  ^blksize_t blksize
  ^gid_t gid
  ^in_addr_t in_addr
  ^in_port_t in_port
  ^ino_t ino
  ^ino64_t ino64
  ^key_t key
  ^mode_t mode
  ^nlink_t nlink
  ^id_t id
  ^pid_t pid
  ^off_t off
  ^swblk_t swblk
  ^uid_t uid
  ^clock_t clock
  ^size_t size
  ^ssize_t ssize
  ^time_t time
  ^cc_t cc
  ^speed_t speed
  ^tcflag_t tcflag
  ^fsblkcnt_t fsblkcnt
  ^fsfilcnt_t fsfilcnt
  ^sa_family_t sa_family
  ^socklen_t socklen
  ^rlim_t rlim)

(library/define-interface omkamra_jnr_test_interface
  (^int omkamra_jnr_test_123 [])

  ;; use `char` for both signed and unsigned char
  (^char omkamra_jnr_test_add_chars [^char x ^char y])

  ;; use `short` for both signed and unsigned short
  (^short omkamra_jnr_test_add_shorts [^short x ^short y])

  ;; use `int` for both signed and unsigned int
  (^int omkamra_jnr_test_add_ints [^int x ^int y])

  ;; use `long` for both signed and unsigned long
  ;; 
  ;; note that these are C longs (32 or 64 bit),
  ;; not Java longs (which are always 64 bit)
  (^long omkamra_jnr_test_add_longs [^long x ^long y])

  ;; use `long-long` for both signed and unsigned long long
  (^long-long
   omkamra_jnr_test_add_long_longs [^long-long x
                                    ^long-long y])

  (^float omkamra_jnr_test_add_floats [^float x ^float y])
  (^double omkamra_jnr_test_add_doubles [^double x ^double y])

  ;; for String parameters the zero terminator is supplied automatically
  (^int omkamra_jnr_test_string_length [^String s])

  ;; use `Encoding` annotation to specify how to encode a Unicode string into native bytes
  (^int omkamra_jnr_test_string_length_latin2 [^String ^{:encoding "iso-8859-2"} s])
  (^String omkamra_jnr_test_return_string [])

  (^void omkamra_jnr_test_set_errno [^int errno])
  (^void omkamra_jnr_test_fill_buffer [^ByteBuffer buf ^int size])
  (^int omkamra_jnr_test_enums [^omkamra_jnr_test_enum value])
  (^void omkamra_jnr_test_fill_struct [^Pointer s]))

(def ^:dynamic $testlib nil)

(defn compile-shared-library
  [target-path source]
  (let [status (sh "gcc" "-x" "c" "-shared" "-o" target-path "-" :in (jio/reader source))]
    (assert (zero? (:exit status)))))

(defn with-compiled-test-library
  [run-tests]
  (let [so-file (File/createTempFile "libomkamra_jnr_test_" ".so")
        so-path (.getAbsolutePath so-file)]
    (try
      (compile-shared-library so-path (jio/resource "omkamra_jnr_test.c"))
      (binding [$testlib (library/load omkamra_jnr_test_interface so-path)]
        (run-tests))
      (finally (.delete so-file)))))

(use-fixtures :once with-compiled-test-library)

(deftest check-library
  (is (instance? omkamra_jnr_test_interface $testlib)))

(deftest omkamra_jnr_test_123
  (is (= 123 (.omkamra_jnr_test_123 $testlib))))

(deftest omkamra_jnr_test_add_chars
  (is (= 7 (.omkamra_jnr_test_add_chars $testlib 3 4)))
  (is (= 7 (.omkamra_jnr_test_add_chars $testlib 0xff 8)))
  (is (= -0x78 (.omkamra_jnr_test_add_chars $testlib 0x78 16))))

(deftest omkamra_jnr_test_add_shorts
  (is (= 7 (.omkamra_jnr_test_add_shorts $testlib 3 4)))
  (is (= 0x4578 (.omkamra_jnr_test_add_shorts $testlib 0x1234 0x3344)))
  (is (= 7 (.omkamra_jnr_test_add_shorts $testlib 0xffff 8)))
  (is (= -0x7ff8 (.omkamra_jnr_test_add_shorts $testlib 0x7ff8 16))))

(deftest omkamra_jnr_test_add_ints
  (is (= 7 (.omkamra_jnr_test_add_ints $testlib 3 4)))
  (is (= 0x235689bc (.omkamra_jnr_test_add_ints $testlib 0x12345678 0x11223344)))
  (is (= 7 (.omkamra_jnr_test_add_ints $testlib 0xffffffff 8)))
  (is (= -0x7ffffff8 (.omkamra_jnr_test_add_ints $testlib 0x7ffffff8 16))))

(case (.longSize (jnr.ffi.Runtime/getSystemRuntime))
  8
  (deftest omkamra_jnr_test_add_longs
    (is (= 7 (.omkamra_jnr_test_add_longs $testlib 3 4)))
    (is (= 7 (.omkamra_jnr_test_add_longs $testlib 0xffffffffffffffff 8)))
    (is (= -0x7ffffffffffffff8 (.omkamra_jnr_test_add_longs $testlib 0x7ffffffffffffff8 16))))
  4
  (deftest omkamra_jnr_test_add_longs
    (is (= 7 (.omkamra_jnr_test_add_longs $testlib 3 4)))
    (is (= 7 (.omkamra_jnr_test_add_ints $testlib 0xffffffff 8)))
    (is (= -0x7ffffff8 (.omkamra_jnr_test_add_ints $testlib 0x7ffffff8 16)))))

(deftest omkamra_jnr_test_add_long_longs
  (is (= 7 (.omkamra_jnr_test_add_long_longs $testlib 3 4)))
  (is (= 7 (.omkamra_jnr_test_add_long_longs $testlib -1 8)))
  (is (= -0x7ffffffffffffff8 (.omkamra_jnr_test_add_long_longs $testlib 0x7ffffffffffffff8 16))))

(deftest omkamra_jnr_test_add_floats
  (is (= 5.75 (.omkamra_jnr_test_add_floats $testlib 3.5 2.25))))

(deftest omkamra_jnr_test_add_doubles
  (is (= 5.75 (.omkamra_jnr_test_add_doubles $testlib 3.5 2.25))))

(deftest omkamra_jnr_test_string_length
  (is (= 13 (.omkamra_jnr_test_string_length $testlib "Hello, world!")))
  ;; Java strings are encoded as UTF-8 on the native side
  (is (= 18 (.omkamra_jnr_test_string_length $testlib "áéíóúöüőű"))))

(deftest omkamra_jnr_test_string_length_latin2
  (is (= 13 (.omkamra_jnr_test_string_length_latin2 $testlib "Hello, world!")))
  (is (= 9 (.omkamra_jnr_test_string_length_latin2 $testlib "áéíóúöüőű"))))

(deftest omkamra_jnr_test_return_string
  (is (= "FlyLo/Reggie/Marc" (.omkamra_jnr_test_return_string $testlib))))

(deftest omkamra_jnr_test_set_errno
  (.omkamra_jnr_test_set_errno $testlib 1234)
  (is (= 1234 (library/errno $testlib))))

(deftest omkamra_jnr_test_fill_buffer
  (let [a (byte-array 20)
        buf (ByteBuffer/wrap a)]
    (java.util.Arrays/fill a (byte \.))
    (.omkamra_jnr_test_fill_buffer $testlib buf 16)
    (is (= (mapv byte "ABCDEFGHIJKLMNOP....") (vec a)))))

(deftest omkamra_jnr_test_enums
  (is (= 0 (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_OK)))
  (is (= 1 (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_FAIL)))
  (is (= 12 (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_WHO)))
  (is (= 13 (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_KNOWS)))
  (is (= 23 (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_SURREY)))
  (is (= 24 (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_LAST)))

  (is (= (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_OK)
         (.omkamra_jnr_test_enums $testlib omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_OK)))
  (is (= (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_FAIL)
         (.omkamra_jnr_test_enums $testlib omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_FAIL)))
  (is (= (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_WHO)
         (.omkamra_jnr_test_enums $testlib omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_WHO)))
  (is (= (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_KNOWS)
         (.omkamra_jnr_test_enums $testlib omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_KNOWS)))
  (is (= (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_SURREY)
         (.omkamra_jnr_test_enums $testlib omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_SURREY)))
  (is (= (.intValue omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_LAST)
         (.omkamra_jnr_test_enums $testlib omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_LAST))))

(deftest omkamra_jnr_test_fill_struct
  (let [s (omkamra_jnr_test_struct. (library/runtime $testlib))]
    (.omkamra_jnr_test_fill_struct $testlib (jnr.ffi.Struct/getMemory s))
    (is (= 0x01 (.. s c get)))
    (is (= 0x02 (.. s uc get)))
    (is (= 0x0304 (.. s s get)))
    (is (= 0x0506 (.. s us get)))
    (is (= 0x0708090a (.. s i get)))
    (is (= 0x0b0c0d0e (.. s ui get)))
    (is (= 0x0b0c0d0e (.. s ui get)))
    (case (.longSize (jnr.ffi.Runtime/getSystemRuntime))
      4 (do (is (= 0x10111213 (.. s l get)))
            (is (= 0x18191a1b (.. s ul get))))
      8 (do (is (= 0x1011121314151617 (.. s l get)))
            (is (= 0x18191a1b1c1d1e1f (.. s ul get)))))
    (is (= 0x2021222324252627 (.. s ll get)))
    (is (= 0x3031323334353637 (.. s ull get)))
    (is (= 129.75 (.. s f get)))
    (is (= -951.125 (.. s d get)))
    (is (= omkamra_jnr_test_enum/OMKAMRA_JNR_TEST_SURREY (.. s e get)))
    (is (= 1234 (.. s p x get)))
    (is (= 5678 (.. s p y get)))
    (let [a (.. s a)
          letters (apply str (map #(char (.get (aget a %))) (range 10)))]
      (is (= "ABCDEFGHIJ" letters)))
    (is (= 9876 (.. s u p x get)))
    (is (= 5432 (.. s u p y get)))))

(struct/define ^:packed omkamra_jnr_test_packed_struct
  ^char c
  ^int i)

(deftest omkamra_jnr_test_packed_structs
  (let [s (omkamra_jnr_test_packed_struct. (library/runtime $testlib))]
    (is (= 0 (.. s c offset)))
    (is (= 1 (.. s i offset)))
    (is (= 5 (jnr.ffi.Struct/size s)))))
