options
{
	language = "CSharp";
}

class DemoJavaDocLexer extends Lexer;
options {
	k=2;
	importVocab = Common;
	exportVocab = JavaDoc;
	filter=true;
}

PARAM
	:	"@param" ' ' ID
	;

EXCEPTION
	:	"@exception" ' ' ID
	;

protected
ID	:	('a'..'z'|'A'..'Z')+
	;

/** This rule simply prevents JAVADOC_CLOSE from being
 *  called for every '*' in a comment.  Calling JAVADOC_CLOSE
 *  will fail for simple '*' and cause an exception, which
 *  is slow.  In other words, the grammar will work without
 *  this rule, but is slower.
 */
STAR:	'*' {$setType(Token.SKIP);}
	;

JAVADOC_CLOSE
	:	"*/" {MultiLexer.selector.pop();}
	;

/** Ignore whitespace inside JavaDoc comments */
NEWLINE
	:	(	"\r\n"  // Evil DOS
		|	'\r'    // Macintosh
		|	'\n'    // Unix (the right way)
		)
		{ newline(); $setType(Token.SKIP); }
	;
