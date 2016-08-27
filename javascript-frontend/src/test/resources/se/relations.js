function main() {

  var a = foo();
  var b = bar();
  
  if (a > b) {
    let x = null;

    if (a < b) { // always false
      x = 0;
    }
    foo(x); // PS x=NULL
    
    if (a >= b) { // always true
      x = 0;
    }
    foo(x); // PS x=ZERO
    
  }

}
