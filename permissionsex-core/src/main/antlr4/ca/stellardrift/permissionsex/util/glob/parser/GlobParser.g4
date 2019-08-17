parser grammar GlobParser;

options {
    tokenVocab = GlobLexer;
}


rootGlob: glob EOF;
glob: element+;

literal: CHARACTER+;
or: OR_START glob (OR_SEPARATOR glob)* OR_END;

element: or | literal;
