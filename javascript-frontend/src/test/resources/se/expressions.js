function main() {

  var x; // PS x=UNDEFINED
  var y = foo();

  x = !42; // PS x=FALSE
  x = !0; // PS x=TRUE
  x = !unknown; // PS x=BOOLEAN
  if (y) {
    x = !y; // PS x=FALSE
  }

  if (!x) {
    foo(x); // PS x=FALSE
  }

  x = typeof foobar == 'undefined';

  if (x) {
    foo(x); // PS x=TRUTHY
  }

  x = (y == null);
  if (x) {
    foo(x); // PS x=TRUTHY
  }

  x = typeof foobar;
  if (x) {
    foo(x); // PS x=TRUTHY
  }

  x = (foobar == null);
  y = (foobar.foobar == null);
  if (x) {
    if (!y) {
      foo(); // PS x=TRUTHY & y=FALSY
    }
  }

  foo(x, y);
}
