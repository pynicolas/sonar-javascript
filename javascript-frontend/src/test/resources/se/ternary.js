function main() {
  var x = foo();
  var y = bar();

  var z = (x == null || y == null)
    ? 1
    : x.foo;  // x=NOT_NULL

  dummyStatement();

}
