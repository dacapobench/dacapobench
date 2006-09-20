options {
	language="Cpp";
//	genHashLines = false;
}

{
	#include <iostream>

	int main(int argc, char **argv)
	{
		// create lexer reading from stdin...
		L lexer(std::cin);
		lexer.done = false;

		while ( ! lexer.done )
		{
			ANTLR_USE_NAMESPACE(antlr)RefToken t = lexer.nextToken();
			cout << "Token: " << t->getText() << endl;
		}
	}
}

class L extends Lexer;

options
{
	// Allow any char but \uFFFF (16 bit -1)
	charVocabulary='\u0000'..'\uFFFE';
}

{
public:
	bool done;

	void uponEOF()
	{
		done = true;
	}
}

ID	:	ID_START_LETTER ( ID_LETTER )*
	;

WS	:	(' '|'\n') {$setType(ANTLR_USE_NAMESPACE(antlr)Token::SKIP);
}
;

protected
	ID_START_LETTER
	:	'$'
	|	'_'
	|	'a'..'z'
	|	'\u0080'..'\ufffe'
	;

protected
	ID_LETTER
	:	ID_START_LETTER
	|	'0'..'9'
	;
