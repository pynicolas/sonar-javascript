function property() {
  var x;
  x.foo;   // Noncompliant
}

function element() {
  var x;
  x[1];    // Noncompliant
}

function unknown() {
  var y;
  foo(y);
  y = foo();
  y.foo;
}

function branch() {
  var z;
  if (cond) {
    z = foo();
  }
  z.foo();   // Noncompliant
}

function class_property() {
  class A {
  }
  A.foo = 42;
}

function var_and_function() {
  x.bar = 24;
  var x;
  function x() {}
  x.foo = 42;
}

function equal_null() {
  var x = foo();

  if (x != null) {
    x.foo();
  }

  if (null != x) {
    x.foo();
  }

  if (x == null) {
    x.foo();    // Noncompliant
  }
}

function equal_undefined() {
  var x = foo();

  if (x == undefined) {
    x.foo();     // Noncompliant
  }
}

function strict_equal_null() {
  var x = foo();

  if (x === null) {
    x.foo();    // FN, Noncompliant
  } else {
    x.foo();
  }

  if (x !== null) {
    x.foo();
  } else {
    x.foo();    // FN, Noncompliant
  }
}

function ternary() {
  var x;
  if (condition) {
    x = foo();
  }

  x ? x.foo() : bar();  // Compliant
}

function duplicated_condition() {
  if (foo("bar")) {
    var x = bar();
  }

  if (foo("bar")) {
    x.foo();   // Noncompliant  FP!
  }
}

function stop_after_NPE() {
  var x;
  if (x.foo &&  // Noncompliant
      x.bar     // OK as we can't reach this point after previous issue
  ) {
  }
}

function loop_at_least_once() {
  var x;

  while (condition()) {
    x = foo();
  }

  x.foo();  // Noncompliant, FP? we should execute loop at least once?
}

function loop_with_condition() {
  var x;

  for(var i = 0; i < arr.length; i++){
    if (arr[i].isSomething) {
      x = foo(arr[i]);
    }
  }
  x.foo();  // Noncompliant (such code is not even compiled by Java as "x" might be not initialized)
}

function loop(arr) {
  var obj;
  for(var i = 0; i < arr.length; i++){
    if(i % 2 == 0){
      obj = foo();
    } else {
      obj.bar();   // Noncompliant, FP
    }
  }
}

function one_condition() {
  var x = foo();

  if (x == null) {
    foo();
  }

  if (x
      && x.foo != null) {  // Ok
  }
}

function one_more() {
  var x = foo();
  while (x != null && i < 10) {
  }

  if (!x) {
    return;
  }

  if (x.foo) {  // Ok
  }
}

function not_null_if_property_accessed() {
  var x = foo();

  if (x.foo) {
    if (x != null) {
    }
    x.foo();   // Ok
  }
}

function tested_copy() {
  var x;

  if (condition) {
    x = foo();
  }

  var copy = x;

  if (!copy) {
    return;
  }

  x.foo(); // Noncompliant, FP
}

function typeof_testing() {
  var x;

  if (condition) {
    x = foo();
  }

  if (typeof x === 'function') {
    x.call();   // Noncompliant, FP
  }

  if (typeof x === 'object') {
    x.call();   // Noncompliant, true issue, x might be null
  }

  var y = foo();

  if (typeof y === 'undefined') {
    y.call();  // FN, Noncompliant
  }

}

function assignment_left_first() {
  var x;

  foo[x=foo()] = foo(x.bar);  // Compliant, we first evaluate LHS of assignment
}

function logical_expression_ternary() {
  var x = foo(), y = bar();

  var z = (x == null || y == null)
    ? 1
    : x.foo;  // Noncompliant, FP

  if ((x == null || y == null)) {
    return 1;
  } else {
    return x.foo;  // Noncompliant, FP
  }
}

function null_and_not_undefined() {
  var x = null;

  while (condition()) {
    if (x === null) {
      x = new Obj();
    }
    x.foo();   // Noncompliant, FP
  }
}
