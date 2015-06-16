op: +, -, *, /, %, **

logic op: ==, <, >, <=, >=, ~

a op b
-----------
a = a op b

a b
--------
a = a * b

a+
-----
a + 1

a++
-----
a + 2

~a
--
a = ~a

(f(a, b) == 2) && (b > a) ?
	a b; a - d
!
	c + d; c d
.
------------------------------
if (f(a, b) == 2) && (b > a) {
	a = a * b;
	a = a - d;
} else {
	c = c + d;
	c = c * d;
}

int bobeVariable = 0;
bool rubensVariable = t;
$a = 13;

t, True, true, >0;
f, False, false, 0;


$a = 0
$b = 0
|a
|b
b+
a > b @
	a+
	a|
.

-- single line comment
{ a 

multiline 

{nested}

comment }

# .
local scope
