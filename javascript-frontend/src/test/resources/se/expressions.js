function main() {
  
  var x; // PS x=UNDEFINED
  var y = foo();
  
  x = !42; // PS x=FALSE
  x = !0; // PS x=TRUE
  x = !unknown; // PS x=BOOLEAN
  if (y) {
    x = !y; // PS x=FALSE
  }
  foo(x);
}
