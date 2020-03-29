lexer grammar CommandLexer;

// Quoted strings

DOUBLE_QUOTE: '"';
SINGLE_QUOTE: '\'';
WS: [ \t];

// Flags
LONG_FLAG_START: '--';
FLAG_START: '-';

LONG_FLAG_VALUE_DELIMITER: '=';

fragment LITERAL: .;
fragment ESCAPE_START: '\\';

ESCAPE: ESCAPE_START (DOUBLE_QUOTE | SINGLE_QUOTE | FLAG_START) { setText(getText().substring(1)); };

CHARACTER: ESCAPE | LITERAL;
