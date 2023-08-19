#include <stdint.h>
#include <string.h>
#include <errno.h>

int omkamra_jnr_test_123() {
  return 123;
}

char omkamra_jnr_test_add_chars(char x, char y) { return x + y; }
short omkamra_jnr_test_add_shorts(short x, short y) { return x + y; }
int omkamra_jnr_test_add_ints(int x, int y) { return x + y; }
long omkamra_jnr_test_add_longs(long x, long y) { return x + y; }
long long omkamra_jnr_test_add_long_longs(long long x, long long y) { return x + y; }
float omkamra_jnr_test_add_floats(float x, float y) { return x + y; }
double omkamra_jnr_test_add_doubles(double x, double y) { return x + y; }

int omkamra_jnr_test_string_length(char *s) {
  return strlen(s);
}

int omkamra_jnr_test_string_length_latin2(char *s) {
  return strlen(s);
}

char *omkamra_jnr_test_return_string(int i) {
  switch (i) {
  case 0:
    return "FlyLo";
  case 1:
    return "Reggie";
  case 2:
    return "Marc";
  }
}

void omkamra_jnr_test_set_errno(int new_errno) {
  errno = new_errno;
}

void omkamra_jnr_test_fill_buffer(char *buf, int size) {
  for (int i = 0; i < size; i++) {
    buf[i] = 'A' + i;
  }
}

enum omkamra_jnr_test_enum {
  OMKAMRA_JNR_TEST_OK,
  OMKAMRA_JNR_TEST_FAIL,
  OMKAMRA_JNR_TEST_WHO = 12,
  OMKAMRA_JNR_TEST_KNOWS,
  OMKAMRA_JNR_TEST_SURREY = 23,
  OMKAMRA_JNR_TEST_LAST
};

int omkamra_jnr_test_enums(enum omkamra_jnr_test_enum value) {
  switch (value) {
  case OMKAMRA_JNR_TEST_OK:
    return OMKAMRA_JNR_TEST_OK;
  case OMKAMRA_JNR_TEST_FAIL:
    return OMKAMRA_JNR_TEST_FAIL;
  case OMKAMRA_JNR_TEST_WHO:
    return OMKAMRA_JNR_TEST_WHO;
  case OMKAMRA_JNR_TEST_KNOWS:
    return OMKAMRA_JNR_TEST_KNOWS;
  case OMKAMRA_JNR_TEST_SURREY:
    return OMKAMRA_JNR_TEST_SURREY;
  default:
    return OMKAMRA_JNR_TEST_LAST;
  }
}

struct omkamra_jnr_test_point {
  int x;
  int y;
};

union omkamra_jnr_test_union {
  char c;
  short s;
  int i;
  long l;
  float f;
  double d;
  struct omkamra_jnr_test_point p;
};

struct omkamra_jnr_test_struct {
  char c;
  unsigned char uc;
  short s;
  unsigned short us;
  int i;
  unsigned int ui;
  long l;
  unsigned long ul;
  long long ll;
  unsigned long long ull;
  float f;
  double d;
  enum omkamra_jnr_test_enum e;
  struct omkamra_jnr_test_point p;
  union omkamra_jnr_test_union u;
  char *str;
};

void omkamra_jnr_test_fill_struct(struct omkamra_jnr_test_struct *s) {
  s->c = 0x01;
  s->uc = 0x02;
  s->s = 0x0304;
  s->us = 0x0506;
  s->i = 0x0708090a;
  s->ui = 0x0b0c0d0e;
  if (sizeof(long) == 4) {
    s->l = 0x10111213;
    s->ul = 0x18191a1b;
  } else {
    s->l = 0x1011121314151617;
    s->ul = 0x18191a1b1c1d1e1f;
  }
  s->ll = 0x2021222324252627;
  s->ull = 0x3031323334353637;
  s->f = 129.75;
  s->d = -951.125;
  s->e = OMKAMRA_JNR_TEST_SURREY;
  s->p.x = 1234;
  s->p.y = 5678;
  s->u.p.x = 9876;
  s->u.p.y = 5432;
  s->str = "FFFargo";
}

struct omkamra_jnr_test_struct_with_arrays_inside {
  char c[111];
  unsigned char uc[222];
  short s[333];
  unsigned short us[444];
  int i[555];
  unsigned int ui[666];
  long l[777];
  unsigned long ul[888];
  long long ll[999];
  unsigned long long ull[1111];
  float f[2222];
  double d[3333];
  enum omkamra_jnr_test_enum e[4444];
  struct omkamra_jnr_test_point p[5555];
  union omkamra_jnr_test_union u[6666];
  char *str[7777];
};

void omkamra_jnr_test_fill_struct_with_arrays_inside(
    struct omkamra_jnr_test_struct_with_arrays_inside *s) {
  for (int i = 0; i < 111; i++)
    s->c[i] = i;
  for (int i = 0; i < 222; i++)
    s->uc[i] = i;
  for (int i = 0; i < 333; i++)
    s->s[i] = i;
  for (int i = 0; i < 444; i++)
    s->us[i] = i;
  for (int i = 0; i < 555; i++)
    s->i[i] = i;
  for (int i = 0; i < 666; i++)
    s->ui[i] = i;
  for (int i = 0; i < 777; i++)
    s->l[i] = i;
  for (int i = 0; i < 888; i++)
    s->ul[i] = i;
  for (int i = 0; i < 999; i++)
    s->ll[i] = i;
  for (int i = 0; i < 1111; i++)
    s->ull[i] = i;
  for (int i = 0; i < 2222; i++)
    s->f[i] = i;
  for (int i = 0; i < 3333; i++)
    s->d[i] = i;
  for (int i = 0; i < 4444; i++) {
    switch (i % 3) {
    case 0:
      s->e[i] = OMKAMRA_JNR_TEST_WHO;
      break;
    case 1:
      s->e[i] = OMKAMRA_JNR_TEST_KNOWS;
      break;
    case 2:
      s->e[i] = OMKAMRA_JNR_TEST_SURREY;
      break;
    }
  }
  for (int i = 0; i < 5555; i++) {
    s->p[i].x = i * 2;
    s->p[i].y = i * 3;
  }
  for (int i = 0; i < 6666; i++) {
    switch (i % 7) {
    case 0:
      s->u[i].c = i % 128;
      break;
    case 1:
      s->u[i].s = i;
      break;
    case 2:
      s->u[i].i = i;
      break;
    case 3:
      s->u[i].l = i;
      break;
    case 4:
      s->u[i].f = i;
      break;
    case 5:
      s->u[i].d = i;
      break;
    case 6:
      s->u[i].p.x = i * 2;
      s->u[i].p.y = i * 3;
      break;
    }
  }
  for (int i = 0; i < 7777; i++) {
    s->str[i] = omkamra_jnr_test_return_string(i % 3);
  }
}

void omkamra_jnr_test_pass_by_reference(unsigned char *c, unsigned short *s,
                                        unsigned int *i, unsigned long *l,
                                        unsigned long long *ll, float *f,
                                        double *d, char **p) {
  *c = (*c ^ -1) + 1;
  *s = (*s ^ -1) + 1;
  *i = (*i ^ -1) + 1;
  *l = (*l ^ -1) + 1;
  *ll = (*ll ^ -1) + 1;
  *f += 0.5;
  *d += 0.75;
  *p = "Booze Design";
}
