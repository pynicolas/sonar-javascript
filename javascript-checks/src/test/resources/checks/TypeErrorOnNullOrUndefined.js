function empty() {}

function f1() {
  global1 = null;
  foo(global1.x); // OK
}

function f2() {
  var a;
  a = 1;
  foo(a.x);
  a = null;
  foo(a.x); // Noncompliant [[sc=7;ec=8]] {{"a" is null or undefined}}
}

function f2() {
  var a = 1;
  foo(a.x);
  a = undefined;
  foo(a.x); // Noncompliant
}

function f3() {
  var a;
  foo(a);
  foo(a.x); // Noncompliant
}

function f4() {
  var a;
  foo(a);
  foo(a[x]); // Noncompliant
}

function f5() {
  var a;
  (function () {
    a = 1;
  }());
  foo(a.x); // OK  
}

function f6() {
  var a;
  (function () {
    b = a;
  }());
  foo(a.x); // Noncompliant
}

function f7() {
  var a;
  a.x.y; // Noncompliant
}

function function_parameter(param) {
  param.x;
  param = null;
  param.x; // Noncompliant
}

function function_arguments() {
  arguments.length; // OK
}

function nested_function_declaration() {
  function nested() {}
  nested.x;
}

var functionWithoutBlock = x => 1;

function basic_if() {
  var a;
  if (condition) {
    a = 1;
  }
  a.x; // Noncompliant
}

function basic_while() {
  var a;
  while(condition()) {
    a = random();
    a.x;
  }
}

function for_in() {
  for (var a in obj) {
    foo(a.x);
  }
  var b;
  for (b of obj) {
    foo(b.x);
  }
}

function catch_exception() {
  try { 
    foo();
  } catch (e) {
    bar(e.message);
  }
}
