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

char *omkamra_jnr_test_return_string() {
  return "FlyLo/Reggie/Marc";
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
  int a[10];
  union omkamra_jnr_test_union u;
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
  for (int i=0; i<10; i++) {
    s->a[i] = 'A' + i;
  }
  s->u.p.x = 9876;
  s->u.p.y = 5432;
}

void omkamra_jnr_test_pass_by_reference(unsigned char *c, unsigned short *s,
                                        unsigned int *i, unsigned long *l,
                                        unsigned long long *ll, float *f, double *d,
                                        char **p) {
  *c = (*c ^ -1) + 1;
  *s = (*s ^ -1) + 1;
  *i = (*i ^ -1) + 1;
  *l = (*l ^ -1) + 1;
  *ll = (*ll ^ -1) + 1;
  *f += 0.5;
  *d += 0.75;
  *p = "Booze Design";
}
