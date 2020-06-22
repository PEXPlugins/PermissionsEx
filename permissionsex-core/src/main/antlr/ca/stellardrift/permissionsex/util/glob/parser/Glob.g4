grammar Glob;

@header {
package ca.stellardrift.permissionsex.util.glob.parser;
}

rootGlob: glob EOF;

glob: element+;

element: or
    | chars
    | unit=LITERAL;

/** Each single character is one possibility */
chars: '[' content=LITERAL ']';

/* Choose one of the subsequences */
or: '{' (glob ','?)* '}';


LITERAL: (ESC | CHAR | DIGIT)+;

fragment DIGIT : [0-9];
CHAR: ~('{' | '}' | '[' | ']' | ',');
ESC: '\\' ('{' | '}' | ',');


