myarray = [1, 2, 4]

myarray.sort() // NOK
myarray.sort(sortFunc) // OK



var myarray1 = [1, 2, 4]

myarray1.sort() // NOK
myarray1.sort(sortFunc) // OK



myStrArray = ["a", "b"]
myStrArray.sort() // NOK - false positive!


var a = {sort : function(){}}
a.sort() // OK


b.sort() // OK