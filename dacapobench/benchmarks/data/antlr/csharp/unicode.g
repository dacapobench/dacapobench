options
{
	language = "CSharp";
}

class L extends Lexer;

options {
	// Allow any char but \uFFFF (16 bit -1)
	charVocabulary='\u0000'..'\uFFFE';
}

{
	private static bool done = false;

    public override void uponEOF() {
		done=true;
    }
	
	public static void Main(string[] args) {
		L lexer = new L(new CharBuffer(Console.In));
		while ( !done ) {
			Token t = lexer.nextToken();
			Console.Out.WriteLine("Token: "+t);
		}
	}
}

ID	:	ID_START_LETTER ( ID_LETTER )*
	;

WS	:	(' '|'\n') {$setType(Token.SKIP);}
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
