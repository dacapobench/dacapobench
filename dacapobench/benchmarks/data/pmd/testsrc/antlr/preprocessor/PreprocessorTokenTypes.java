// $ANTLR : "preproc.g" -> "Preprocessor.java"$

package antlr.preprocessor;

public interface PreprocessorTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int LITERAL_tokens = 4;
	int HEADER_ACTION = 5;
	int ACTION = 6;
	int LITERAL_class = 7;
	int ID = 8;
	int LITERAL_extends = 9;
	int SEMI = 10;
	int TOKENS_SPEC = 11;
	int OPTIONS_START = 12;
	int ASSIGN_RHS = 13;
	int RCURLY = 14;
	int LITERAL_protected = 15;
	int LITERAL_private = 16;
	int LITERAL_public = 17;
	int BANG = 18;
	int ARG_ACTION = 19;
	int LITERAL_returns = 20;
	int RULE_BLOCK = 21;
	int LITERAL_throws = 22;
	int COMMA = 23;
	int LITERAL_exception = 24;
	int LITERAL_catch = 25;
	int SUBRULE_BLOCK = 26;
	int ALT = 27;
	int ELEMENT = 28;
	int ID_OR_KEYWORD = 29;
	int CURLY_BLOCK_SCARF = 30;
	int WS = 31;
	int NEWLINE = 32;
	int COMMENT = 33;
	int SL_COMMENT = 34;
	int ML_COMMENT = 35;
	int CHAR_LITERAL = 36;
	int STRING_LITERAL = 37;
	int ESC = 38;
	int DIGIT = 39;
	int XDIGIT = 40;
}
