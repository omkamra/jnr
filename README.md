# omkamra.jnr

A Clojure wrapper around [JNR-FFI](https://github.com/jnr/jnr-ffi).

Makes it possible to call functions in native libraries without
writing explicit wrappers through JNI, SWIG, etc.

So far only tested on 64-bit Linux.

## Usage

```clojure
(ns com.example
  (:require [omkamra.jnr.library :as library]
            [omkamra.jnr.struct :as struct]
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
| `char` | `^char` `^byte` |
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
| `off_t` | `^off_t` |
| `size_t` | `^size_t` |
| `ssize_t` | `^ssize_t` |

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

The Unicode string is encoded into a temporary buffer and `strlen`
gets a pointer to the encoded bytes. The encoding is UTF-8 by default
but may be overridden via the `Encoding` annotation:

```clojure
  (^size_t strlen [^{:tag String jnr.ffi.annotations.Encoding "iso-8859-2"}])
```

The terminating zero byte is automatically supplied by the FFI.

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
  ^{:tag int jnr.ffi.types.time_t true} tv_sec
  ^long tv_nsec)

(library/define $c "c"
  (^int clock_gettime [^int clock ^Pointer ts]))

(def CLOCK_REALTIME 0)

(let [ts (timespec. (library/runtime $c))]
  (.clock_gettime $c CLOCK_REALTIME (timespec/getMemory ts))
  (printf "%d seconds, %d nanoseconds\n"
           (.. ts tv_sec get)
           (.. ts tv_nsec get)))
```

Here we:

1. dynamically create a new `jnr.ffi.Struct` subclass: `(struct/define timespec ...)`,
2. instantiate it: `(timespec. (library/runtime $c))` and
3. take a pointer to the struct instance: `(timespec/getMemory ts)`.

Check the `jnr.ffi.types` package for a list of type annotations
predefined by JNR-FFI. These are automatically converted to the right
underlying type depending on the platform.

Unfortunately `clockid_t` - the type of the first `clock_gettime()`
argument - is not supported yet so I had to tag with a plain `int` and
hope for the best.

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
                           ^{:tag jnr.ffi.byref.FloatByReference jnr.ffi.annotations.Out true} ddpi
                           ^{:tag jnr.ffi.byref.FloatByReference jnr.ffi.annotations.Out true} hdpi
                           ^{:tag jnr.ffi.byref.FloatByReference jnr.ffi.annotations.Out true} vdpi])
  (^void SDL_VideoQuit []))

(let [ddpi (jnr.ffi.byref.FloatByReference.)
      hdpi (jnr.ffi.byref.FloatByReference.)
      vdpi (jnr.ffi.byref.FloatByReference.)]
  (assert (zero? (.SDL_VideoInit $sdl2 nil)))
  (.SDL_GetDisplayDPI $sdl2 0 ddpi hdpi vdpi)
  (.SDL_VideoQuit $sdl2)
  (printf "ddpi: %f hdpi: %f vdpi: %f\n"
          (.floatValue ddpi)
          (.floatValue hdpi)
          (.floatValue vdpi)))
```

The `Out` annotation tells the FFI that `SDL_GetDisplayDPI()` will
only write through the annotated pointers. This is an optimization
which avoids a JVM->C copy before making the call. There is also a
corresponding `In` annotation. The default is both `In` and `Out`.

`jnr.ffi.byref.FloatByReference` and `jnr.ffi.annotations.Out`
can be shortened to `FloatByReference` and `Out` by importing these
classes in the namespace declaration.

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

Copyright © 2021 Balázs Ruzsa

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
