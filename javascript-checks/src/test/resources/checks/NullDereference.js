function main() {
  var x;
  x.foo(); // Noncompliant
  x.foo;   // Noncompliant
  x[1];    // Noncompliant

  var y;
  foo(y);
  y = foo();
  y.foo;

  var z;
  if (cond) {
    z = foo();
  }
  z.foo();   // Noncompliant
}
