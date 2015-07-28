lexer grammar GlobLexer;

OR_START: '{';
OR_SEPARATOR: ',';
OR_END: '}';


fragment LITERAL: .;
fragment ESCAPE_START: '\\';
ESCAPE: ESCAPE_START (OR_START | OR_SEPARATOR | OR_END) { setText(getText().substring(1)); };

CHARACTER: ESCAPE | LITERAL;
