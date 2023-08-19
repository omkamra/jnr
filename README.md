# omkamra.jnr

[![Clojars Project](https://img.shields.io/clojars/v/com.github.omkamra/jnr.svg)](https://clojars.org/com.github.omkamra/jnr)

A Clojure wrapper around [JNR-FFI](https://github.com/jnr/jnr-ffi).

Makes it possible to call functions in native libraries without
writing explicit wrappers through JNI, SWIG, etc.

So far only tested on 64-bit Linux.

## Usage

```clojure
(ns com.example
  (:require [omkamra.jnr.library :as library]
            [omkamra.jnr.struct :as struct]
            [omkamra.jnr.union :as union]
            [omkamra.jnr.enum :as enum]
            [omkamra.jnr.util :as util)))
```

C:

```c
#include <stdlib.h>

void *p = malloc(1024);
free(p);
```

Clojure:

```clojure
(library/define $c "c"
  (^Pointer malloc [^size_t size])
  (^void free [^Pointer p]))

(let [p (.malloc $c 1024)]
  (.free $c p))
```

The `library/define` form creates a var called `$c` in the current
namespace and binds it to an object which implements the interface
consisting of the listed methods. Each method corresponds to a native
function exported by the library `"c"` whose actual name is dependent
on the platform (`libc.so` on Linux, `c.dll` on Windows, etc.)

Types of function parameters and return values are described via
metadata:

| C type | Clojure metadata |
| ------ | ---------------- |
| `void` | `^void` |
| `char` | `^char` or `^byte` |
| `short` | `^short` |
| `int` | `^int` |
| `long` | `^long` |
| `long long` | `^long-long` |
| `float` | `^float` |
| `double` | `^double` |
| `<type>*` | `^Pointer` |
| `char*` | `^String` |
| `int8_t`..`int64_t` | `^int8_t`..`^int64_t` |
| `uint8_t`..`uint64_t` | `^uint8_t`..`^uint64_t` |

Besides these primitive types you can also use aliases like `size_t`,
`off_t`, `pid_t` - for a full list check the [JNR-FFI
sources](https://github.com/jnr/jnr-ffi/tree/master/src/main/java/jnr/ffi/types).

Note that `long` is a C long (32/64 bits depending on the platform),
not a JVM long (which is always 64 bits).

The JVM does not make a distinction between signed and unsigned types.

Pointers are typeless.

### Strings

C:

```c
#include <stdio.h>
#include <string.h>

size_t len = strlen("Hello, world!");
printf("Length of the string is %d\n", len);
```

Clojure:

```clojure
(library/define $c "c"
  (^size_t strlen [^String s]))

(let [len (.strlen $c "Hello, world!")]
  (printf "Length of the string is %d\n" len))
```

The Unicode string on the JVM side is encoded into a temporary buffer
and `strlen` gets a pointer to the encoded bytes. The encoding is
UTF-8 by default but can be overridden by supplying an `:encoding`
option:

```clojure
  (^size_t strlen [^String ^{:encoding "iso-8859-2"} s])
```

The terminating zero byte on the native side is automatically supplied
by the FFI.

### Buffers

C:

```c
#include <stdio.h>

char buf[4096];

FILE *f = fopen("/dev/urandom", "r");
fread(buf, 4096, 1, f);
fclose(f);
```

Clojure:

```clojure
(library/define $c "c"
  (^Pointer fopen [^String filename ^String opentype])
  (^size_t fread [^java.nio.Buffer data ^size_t size ^size_t count ^Pointer stream])
  (^int fclose [^Pointer stream]))

(let [buf (java.nio.ByteBuffer/allocate 4096)
      f (.fopen $c "/dev/urandom" "r")]
  (.fread $c buf 4096 1 f)
  (.fclose $c f))
```

If you have a `java.nio.Buffer` argument but the parameter is tagged
as a plain `Pointer`, you must wrap the buffer in a Pointer:

```clojure
(jnr.ffi.Pointer/wrap (library/runtime $c) buf)
```

### Structures

C:

```c
#include <time.h>
#include <stdio.h>

struct timespec ts;
clock_gettime(CLOCK_REALTIME, &ts);

printf("%d seconds, %ld nanoseconds\n", ts.tv_sec, ts.tv_nsec);
```

Clojure:

```clojure
(struct/define timespec
  ^time_t tv_sec
  ^long tv_nsec)

(library/define $c "c"
  (^int clock_gettime [^int clock ^timespec ts]))

(def CLOCK_REALTIME 0)

(let [ts (timespec. (library/runtime $c))]
  (.clock_gettime $c CLOCK_REALTIME ts)
  (printf "%d seconds, %d nanoseconds\n"
           (.. ts tv_sec get)
           (.. ts tv_nsec get)))
```

Here we:

1. dynamically create a new `jnr.ffi.Struct` subclass: `(struct/define timespec ...)`,
2. instantiate it: `(timespec. (library/runtime $c))`
3. pass it to `clock_gettime()`

Before the call, the FFI creates a native `timespec` structure (laid
out in memory according to platform conventions) and populates its
fields from the JVM-side `ts` object. The `clock_gettime()` function
gets a pointer to this native structure. After the call returns, all
fields in the native structure are copied back to the `ts` object. If
you want to avoid the copying in either direction, add an `:in` or
`:out` option to the relevant struct parameter:

```clojure
(^int clock_gettime [^int clock ^timespec ^:out ts])
```

### Unions

C:

```c
typedef union epoll_data
{
  void *ptr;
  int fd;
  uint32_t u32;
  uint64_t u64;
} epoll_data_t;

struct epoll_event
{
  uint32_t events;      /* Epoll events */
  epoll_data_t data;    /* User data variable */
} __attribute__ ((__packed__));
```

Clojure:

```clojure
(union/define epoll_data_t
  ^Pointer ptr
  ^int fd
  ^uint32_t u32
  ^uint64_t u64)

(struct/define ^:packed epoll_event
  ^uint32_t events
  ^epoll_data_t data)
```

### Passing primitive types by reference

C:

```c
extern int SDL_VideoInit(const char *driver_name);
extern int SDL_GetDisplayDPI(int displayIndex, float * ddpi, float * hdpi, float * vdpi);
extern void SDL_VideoQuit(void);
```

Clojure:

```clojure
(library/define $sdl2 "SDL2"
  (^int SDL_VideoInit [^String driver_name])
  (^int SDL_GetDisplayDPI [^int displayIndex
                           ^Float* ^:out ddpi
                           ^Float* ^:out hdpi
                           ^Float* ^:out vdpi])
  (^void SDL_VideoQuit []))

(let [ddpi (jnr.ffi.byref.FloatByReference.)
      hdpi (jnr.ffi.byref.FloatByReference.)
      vdpi (jnr.ffi.byref.FloatByReference.)]
  (assert (zero? (.SDL_VideoInit $sdl2 nil)))
  (.SDL_GetDisplayDPI $sdl2 0 ddpi hdpi vdpi)
  (.SDL_VideoQuit $sdl2)
  (printf "ddpi: %f hdpi: %f vdpi: %f\n"
          (.getValue ddpi)
          (.getValue hdpi)
          (.getValue vdpi)))
```

Type tags of the form `<X>*` are automagically expanded to
`jnr.ffi.byref.<X>ByReference` at compile time. Possible values of
`<X>` include `Byte`, `Short`, `Int`, `Long`, `LongLong`, `Float`,
`Double` and `Pointer`.

If you want to set a value for an `:in` argument before the call, pass
it to the `XByReference` constructor,
e.g. `(jnr.ffi.byref.FloatByReference. 5.0)`.

### Enumerations

C:

```c
typedef enum {
  UV_RUN_DEFAULT = 0,
  UV_RUN_ONCE,
  UV_RUN_NOWAIT
} uv_run_mode;

if (mode == UV_RUN_ONCE) {
  do_something();
}

```

Clojure:

```clojure
(enum/define uv_run_mode
  [UV_RUN_DEFAULT 0]
  UV_RUN_ONCE
  UV_RUN_NOWAIT)

(if (= mode uv_run_mode/UV_RUN_ONCE)
  (do-something))
```

The `enum/define` form dynamically creates a `java.lang.Enum`
subclass with the given name.

Enum values are assigned in the same way as in C.

### Errno

C:

```c
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>

if (!open("myfile", O_RDWR)) {
  printf("open failed: %s\n", strerror(errno));
}
```

Clojure:

```clojure
(def O_RDONLY 0)
(def O_WRONLY 1)
(def O_RDWR 2)

(library/define $c "c"
  (^int open [^String filename ^int flags])
  (^String strerror [^int errnum]))

(when-not (zero? (.open $c "myfile" O_RDWR))
  (println (.strerror $c (library/errno $c))))
```

## License

Copyright © 2023 Balázs Ruzsa

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
