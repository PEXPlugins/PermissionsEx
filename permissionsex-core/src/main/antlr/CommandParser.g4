parser grammar CommandParser;

/**
 * Commands grammar
 * A series of tokens, which can be either:
 * - Short flag group
 * - Long flag (either with separate arguments, or with a value delimiter of `=`)
 * - A single word, separated by whitespace
 * - A flag delimiter of --. After this point, no flags will be interpreted
 */
options {
    tokenVocab = CommandLexer;
}

command: (flag | word)+ (flagTerminator word+)? EOF;

flag: shortFlag | longFlag;
flagTerminator: LONG_FLAG_START WS;

shortFlag: FLAG_START word;
longFlag: LONG_FLAG_START CHARACTER+ (LONG_FLAG_VALUE_DELIMITER word)?;


word: (quotedString | CHARACTER+ WS);

quotedString: singleQuotedString | doubleQuotedString;

singleQuotedString: SINGLE_QUOTE (CHARACTER | WS)+ SINGLE_QUOTE;
doubleQuotedString: DOUBLE_QUOTE (CHARACTER | WS)+ DOUBLE_QUOTE;

