function main(a, b) {
  if((x || a == (b || d)) && c){
    foo();
  }
  
  (foo);
  foo(a && b);
  bar(a && b);

  if (a) { 
    foo();
  }
  
  if (a && b) { 
    foo();
  }
  
  if (a || b) { 
    foo();
  }
    
  if (a || b)
     foo();

  for (var i = 0; i < 10; i++) {
    foo()
  }

  for (i = 0; i < 10; i++) {
    foo()
  }

  for (var i = 0; condition();) {
    foo()
  }
  
  for (var element in list) {}
  for (element in list) {}
  
  for (var element of list) {}
  for (element of list) {}
    
  switch(a) {
  case b:
    break;
  case c:
  case d:
    foo();
  default:
    bar();
  }
  
  
  while(a && b) {}
  
  for(a || b; cond1 && cond2; i++, j++) {
    foo();
  }

  switch(a || b) {
  case c:
    foo();
  case g:
    baz();
  }

  switch(a) {
  case c || d:
    foo();
  case e && f:
    bar();
  case g:
    baz();
  }


  
}

