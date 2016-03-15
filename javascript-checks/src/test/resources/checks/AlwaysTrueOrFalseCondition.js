function emptyFunctionBody() {}

functionWithoutBlock = x => 1;

function global_variables_should_not_be_tracked() {
  global1 = null;
  if (global1) {} // global1 may have been updated since the assignment at the previous line
}

function unknown_value() {
  var a = random();
  if (a) {}
}

function undefined_variable() {
  var a;
  if (a) {} // Noncompliant  [[sc=7;ec=8]] {{Change this condition so that it does not always evaluate to "false".}}
}

function null_variable() {
  var a = null;
  if (a) {} // Noncompliant
}

function function_parameter(param1) {
  if (param1) {}
  param1 = null;
  if (param1) {} // Noncompliant
}

function function_arguments() {
  arguments = null;
  if (arguments) {} // Noncompliant 
}

function not_condition() {
  var a;
  if (!a) {} // Noncompliant {{Change this condition so that it does not always evaluate to "true".}}
}

function and_condition() {
  var a = random();
  if (a && !a) {} // Noncompliant {{Change this condition so that it does not always evaluate to "false".}}
}

function or_condition() {
  var a = random();
  if (a || !a) {} // Noncompliant {{Change this condition so that it does not always evaluate to "true".}}
}

function loop() {
  var a;
  while (condition()) {
    if (!a) {
      a = 42;
    }
  }
}

function for_in(obj) {
  var a;
  for (var prop in obj) {
    if (prop) {} 
  }
  prop = null;
  for (prop of obj) {
    if (prop) {} 
  }
}

function try_catch() {
  var a;
  try {
    a = random();
    doSomethingWhichMayThrowAnException();
    return a;
  } catch (e) {
    if (a) {}
  } 
}

function big_number_of_paths() {
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  foo(a && b);
  var x;
  if (x) {}
}