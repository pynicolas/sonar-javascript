function main(par, unused) {

  var x;   // PS x=NULL & nested=UNKNOWN & par=UNKNOWN & !unused
  var y = null;  // PS y=NULL
  var z = foo(y); // PS z=UNKNOWN & y=NULL

  nested(par, x, y, z);

  function nested() { }

}
